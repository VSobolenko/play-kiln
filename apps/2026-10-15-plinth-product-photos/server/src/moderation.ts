import { randomUUID } from 'node:crypto';
import type Database from 'better-sqlite3';

export type ReportCategory = 'OFFENSIVE' | 'WRONG_PRODUCT' | 'UNSAFE' | 'RIGHTS' | 'OTHER';

export function recordOutputReport(
  db: Database.Database,
  walletId: string,
  jobId: string,
  category: ReportCategory,
  note?: string,
  now = Date.now(),
): { id: number; caseStatus: string } {
  if (!['OFFENSIVE', 'WRONG_PRODUCT', 'UNSAFE', 'RIGHTS', 'OTHER'].includes(category) ||
      (note !== undefined && note.length > 500)) throw new Error('REPORT_INVALID');
  const job = db.prepare(`SELECT j.op, j.state, j.provider_endpoint, r.sha256
    FROM job j JOIN result r ON r.job_id = j.id AND r.wallet_id = j.wallet_id
    WHERE j.id = ? AND j.wallet_id = ? AND j.state = 'SUCCEEDED'`)
    .get(jobId, walletId) as {
      op: string; state: string; provider_endpoint: string | null; sha256: string;
    } | undefined;
  if (!job) throw new Error('JOB_NOT_FOUND');
  return db.transaction(() => {
    const existingCase = db.prepare('SELECT id, status FROM moderation_case WHERE result_fingerprint = ?')
      .get(job.sha256) as { id: string; status: string } | undefined;
    const caseId = existingCase?.id ?? randomUUID();
    if (!existingCase) {
      db.prepare(`INSERT INTO moderation_case(id, result_fingerprint, report_count, status, created_at, updated_at)
        VALUES (?, ?, 0, 'OPEN', ?, ?)`).run(caseId, job.sha256, now, now);
    }
    const inserted = db.prepare(`INSERT INTO report
      (job_id, wallet_id, category, note, status, provider_endpoint, model_version, result_fingerprint, created_at,
       operation_id)
      VALUES (?, ?, ?, ?, 'OPEN', ?, ?, ?, ?, ?)`)
      .run(jobId, walletId, category, note ?? null, job.provider_endpoint, job.provider_endpoint,
        job.sha256, now, job.op);
    const reportId = Number(inserted.lastInsertRowid);
    const link = db.prepare(`INSERT OR IGNORE INTO moderation_case_report(case_id, wallet_id, report_id)
      VALUES (?, ?, ?)`).run(caseId, walletId, reportId);
    if (link.changes > 0) {
      db.prepare(`UPDATE moderation_case SET report_count = report_count + 1,
        status = CASE WHEN report_count + 1 >= 3 THEN 'ESCALATED' ELSE status END, updated_at = ?
        WHERE id = ?`).run(now, caseId);
    }
    const current = db.prepare('SELECT status FROM moderation_case WHERE id = ?').get(caseId) as
      { status: string };
    return { id: reportId, caseStatus: current.status };
  }).immediate();
}

export function reportStatus(
  db: Database.Database,
  walletId: string,
  reportId: number,
): { id: number; status: string; category: string; createdAt: number; caseStatus: string } | null {
  return db.prepare(`SELECT r.id, r.status, r.category, r.created_at AS createdAt,
      COALESCE(c.status, r.status) AS caseStatus
    FROM report r LEFT JOIN moderation_case c ON c.result_fingerprint = r.result_fingerprint
    WHERE r.id = ? AND r.wallet_id = ?`).get(reportId, walletId) as {
      id: number; status: string; category: string; createdAt: number; caseStatus: string;
    } | undefined ?? null;
}

export function removeWalletReports(db: Database.Database, walletId: string): void {
  db.transaction(() => {
    const cases = db.prepare('SELECT DISTINCT case_id FROM moderation_case_report WHERE wallet_id = ?')
      .all(walletId) as Array<{ case_id: string }>;
    db.prepare('DELETE FROM moderation_case_report WHERE wallet_id = ?').run(walletId);
    for (const item of cases) {
      db.prepare(`UPDATE moderation_case SET report_count = (
          SELECT COUNT(*) FROM moderation_case_report cr WHERE cr.case_id = moderation_case.id
        ), status = CASE WHEN status = 'ESCALATED' AND (
          SELECT COUNT(*) FROM moderation_case_report cr WHERE cr.case_id = moderation_case.id
        ) < 3 THEN 'OPEN' ELSE status END WHERE id = ?`).run(item.case_id);
    }
    db.prepare('DELETE FROM report WHERE wallet_id = ?').run(walletId);
    db.prepare(`DELETE FROM moderation_case WHERE NOT EXISTS (
      SELECT 1 FROM moderation_case_report cr WHERE cr.case_id = moderation_case.id
    )`).run();
  }).immediate();
}
