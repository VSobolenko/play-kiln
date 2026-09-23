# Codex worker operator guide

Use Codex workers for bounded, durable engineering tasks. The runner keeps output in
`.kiln/codex/`; it does not make a worker's self-report authoritative.

## Commands

```bash
# Start one worker. The runner stays in the foreground.
node scripts/codex-run.mjs run --id <id> --prompt-file .kiln/codex/prompts/<prompt>.md

# Inspect all workers or one worker.
node scripts/codex-run.mjs list
node scripts/codex-run.mjs status
node scripts/codex-run.mjs status --id <id>

# Read the final answer or recent logs.
node scripts/codex-run.mjs last --id <id> --chars 4000
node scripts/codex-run.mjs log --id <id> --lines 60

# Continue the exact recorded session with a follow-up prompt.
node scripts/codex-run.mjs resume --id <id> --prompt-file .kiln/codex/prompts/<follow-up>.md
```

`run` requires `--id` and `--prompt-file`. `resume` requires the same worker ID and
a new prompt file. The runner extracts the exact CLI session UUID; never substitute
“last session” when workers share a directory.

## `run` options

| Option | Values | Default | Use |
|---|---|---|---|
| `--cwd <dir>` | Existing working directory | Repository root | Isolate the worker's scope or worktree |
| `--model <name>` | `gpt-5.6-sol`, `gpt-5.6-terra`, `gpt-reserve` | Codex config; factory default model is `gpt-5.6-sol` | Select an available worker model |
| `--effort <level>` | `none`, `low`, `medium`, `high`, `xhigh`, `max` | `medium` | Match reasoning cost to task risk |
| `--sandbox <mode>` | `read-only`, `workspace-write`, `danger-full-access` | `workspace-write` | Restrict filesystem/process authority |
| `--timeout-min <n>` | Positive minute count | No runner timeout | Kill a worker that exceeds a deliberate limit |
| `--label <text>` | Human-readable text | Empty | Explain the worker in status output |

Use `read-only` for Verify. Use `workspace-write` for normal Build/Store work.
`danger-full-access` is available but should not be used unless the task explicitly
requires and authorizes it.

## Prompt contract

A worker prompt must stand alone. Include these sections in this order:

1. **Outcome:** one observable result.
2. **Scope:** exact working directory and exact files it may read or write.
3. **Inputs:** specs and facts it must read first.
4. **Hard rules:** forbidden files/actions, no secrets, and no Git mutations.
5. **Verification boundary:** exact lightweight checks the worker may run.
6. **No-Gradle rule:** “Do not run Gradle; the orchestrator owns Gradle builds” unless
   the worker was explicitly assigned a Gradle verification task.
7. **Token contract:** state the supplied token budget, require the worker to reserve
   enough for verification and handoff, and require `CONTEXT_LIMIT` before it guesses
   or silently drops scope. The runner has no separate token-budget option.
8. **Reply contract:** require a fixed, machine-scannable final form, for example
   “Reply with exactly: `DONE <files>` or `BLOCKED <reason>`.”

Example ending:

```text
Do not run Gradle. Do not edit outside the four files above. Do not run git.
Token contract: stay within the supplied budget; if the work cannot be completed
without dropping scope, reply exactly CONTEXT_LIMIT.
On success reply with exactly: DONE <comma-separated files>.
On failure reply with exactly: BLOCKED <one-line reason>.
```

Do not include secrets in prompts: prompts, metadata, stdout, and stderr are durable
local files.

## Reading output

1. Run `status --id <id>`.
2. On `DONE`, read `last`; then read the diff and execute the required verification.
3. On `FAILED`, `BLOCKED(...)`, `LOST`, or no final message, read `log` and the `.err` tail.
4. Treat the worker's claimed tests as unverified until reproduced by the controller.
5. Use `resume` only when a narrow follow-up can fix the same task safely.

## Ledger verdicts

`status`/`list` derive one verdict per worker from its recorded status, its exit
record, and whether its recorded pid is still alive:

| Verdict | Meaning |
|---|---|
| `DONE` | Exit record with exit code 0 and no diagnosed signal |
| `FAILED(<code>)` | Exit record with a non-zero exit code and no signal |
| `BLOCKED(<signal>)` | Exit record diagnosed a signal; a signal outranks a zero exit code, because a worker that stopped on a limit has not delivered |
| `LOST` | Recorded `running`, no exit record, and the recorded pid is not alive (including no usable recorded pid) |

`DONE` is the runner's process verdict, not evidence the deliverable exists: read the
diff and reproduce the verification. A `LOST` worker died without reporting, so its
work is **unverified** — list and read its files on disk directly, and never assume the
task completed. Liveness is a heuristic with a known pid-reuse limitation: pids are
recycled, so a dead worker's pid can now belong to an unrelated live process and stay
reported `running`; only `ESRCH` proves death, and `EPERM` is treated as alive, so the
check can miss a death but never invents a `LOST`.

## Classified failures

| Failure | Meaning | Reaction |
|---|---|---|
| `RATE_LIMITED` | Usage/rate/quota limit | Do not retry; hand the preserved task to Cursor `generator` |
| `CONTEXT_LIMIT` | Context or token limit | Reduce scope and resume once; then use `generator` |
| `AUTH_ERROR` | CLI login/session problem | Do not create or paste credentials; use `generator` and report the auth issue |

An unclassified failure gets one evidence-driven resume. If the same issue occurs
twice, stop retries and use the ladder in `harness/dual-agent.md`.

## Codex-first availability

A `BLOCKED(...)` label from `list` is not evidence that Codex is unavailable. Before
assigning a Cursor `generator`, run a fresh read-only, trivial `tier-smoke`-shaped
probe worker in this session at the tier the task will use, and record its observed
exit in the handoff. Apply the failure table to the failed worker task only:
`RATE_LIMITED` does not retry and goes to `generator`; `CONTEXT_LIMIT` reduces scope
and resumes once, then goes to `generator`; `AUTH_ERROR` records the blocker and goes
to `generator`. One blocked worker does not disable Codex while the fresh probe
answers.

Historical exit records predate the current classifier and can describe its bug rather
than the worker's fate. Check the deliverable on disk before trusting a label; see
“2026-09-23 - the failure classifier reported finished work as blocked, because it
matched prose” in `harness/lessons.md`.

## Three-worker parallel example

First create three prompts with disjoint write sets:

```text
.kiln/codex/prompts/build-ui.md
.kiln/codex/prompts/store-copy.md
.kiln/codex/prompts/verify-readonly.md
```

From PowerShell, launch the foreground runners as background jobs:

```powershell
$kilnRoot = (Resolve-Path '.').Path
$jobs = @(
  Start-Job -Name build-ui -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/codex-run.mjs run --id build-ui --prompt-file .kiln/codex/prompts/build-ui.md --model gpt-5.6-sol --effort high --sandbox workspace-write --label 'Build UI'
  }
  Start-Job -Name store-copy -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/codex-run.mjs run --id store-copy --prompt-file .kiln/codex/prompts/store-copy.md --model gpt-5.6-terra --effort medium --sandbox workspace-write --label 'Store copy'
  }
  Start-Job -Name verify-readonly -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/codex-run.mjs run --id verify-readonly --prompt-file .kiln/codex/prompts/verify-readonly.md --model gpt-reserve --effort high --sandbox read-only --label 'Read-only verification'
  }
)
```

Poll the durable state, not the PowerShell job output:

```powershell
node scripts/codex-run.mjs status
node scripts/codex-run.mjs last --id build-ui
node scripts/codex-run.mjs last --id store-copy
node scripts/codex-run.mjs last --id verify-readonly
$jobs | Wait-Job | Receive-Job
```

The Verify worker must remain read-only while Build and Store write. If two workers
would edit the same file, serialize them or place them in separate worktrees.
