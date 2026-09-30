#!/usr/bin/env node
/**
 * Render `docs/play-console-walkthrough.md` from `scripts/lib/console-form.json`.
 *
 * The walkthrough is the owner-facing, factory-wide pass over a first Play Console upload. It is
 * generated rather than hand-written because the same model has to answer two consumers at once:
 * this document (Russian, the order of the screens and the reason behind each field) and
 * `scripts/store-kit.py`, which prints `store/console-answers.md` for a slot (English, the exact
 * strings to paste). A hand-written document would restate every field, its cap and its value, and
 * the two copies would drift the first time the console changed.
 *
 * Shape. The owner dictated it on 2026-09-29: "просмотри экран, какие поля что вводить, вот так и
 * должно отображаться всё потом в финальном документе: где поле, что вписать, почему". So the
 * document is a list of screens and nothing else - every screen renders a terse per-field table
 * (`поле | что ввести | почему`) followed by the button that saves it. There is no per-screen code
 * here: the renderer walks `screens`, resolves each screen's fields (inline, or from the model path
 * its `fieldsFrom` names), and prints. A new screen is added by editing the model alone.
 *
 * The facts come from one observed session against the live console (`observed` in the model), from
 * Play articles already cited in this repository, or they are labelled `NEEDS-VERIFICATION`. This
 * script only arranges them; it asserts nothing of its own.
 *
 * Usage:
 *   node scripts/console-walkthrough.mjs           # write docs/play-console-walkthrough.md
 *   node scripts/console-walkthrough.mjs --check   # fail when the committed document has drifted
 */
import { readFileSync, writeFileSync } from 'node:fs'
import path from 'node:path'
import process from 'node:process'
import { fileURLToPath } from 'node:url'

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const MODEL_PATH = path.join(ROOT, 'scripts', 'lib', 'console-form.json')
const CHECK = process.argv.includes('--check')

const model = JSON.parse(readFileSync(MODEL_PATH, 'utf8'))
const DOC_PATH = path.join(ROOT, model.docPath)

/** Resolve `a.b.c` against the model, so a screen can own its fields without copying them. */
function resolvePath(dotted) {
  return String(dotted)
    .split('.')
    .reduce((node, key) => (node == null ? node : node[key]), model)
}

/** The fields of one screen or sub-screen: inline `fields`, or the array `fieldsFrom` names. */
function fieldsOf(entry) {
  if (Array.isArray(entry.fields)) return entry.fields
  if (entry.fieldsFrom) {
    const resolved = resolvePath(entry.fieldsFrom)
    if (Array.isArray(resolved)) return resolved
    throw new Error(`console-form.json: ${entry.id} names fieldsFrom ${entry.fieldsFrom}, which is not an array`)
  }
  return []
}

/** Markdown table cells must not carry pipes or newlines. */
function cell(value) {
  return String(value ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/\|/g, '\\|')
    .replace(/\s*\n+\s*/g, ' ')
    .trim()
}

/**
 * The console's own row label for a field.
 *
 * A field carries its label (`nameRu`) or names the model path that carries it (`nameFrom`). The
 * second form exists so one label is stored once: the optional minor-restriction box's sentence lives
 * on the audience branch that answers the box, and a second copy beside the field had already drifted
 * from it - a drift that matters because the first column of this table promises to be the row a human
 * finds on the screen. A path that resolves to nothing is a render failure rather than an empty cell:
 * a table with a blank first column is a row nobody can act on.
 */
function fieldName(field) {
  if (field.nameFrom) {
    const resolved = resolvePath(field.nameFrom)
    if (resolved == null || String(resolved).trim() === '') {
      throw new Error(`console-form.json: field ${field.id} names nameFrom ${field.nameFrom}, which resolves to nothing`)
    }
    return String(resolved)
  }
  return String(field.nameRu ?? '')
}

/**
 * What the human types or chooses, as the document shows it.
 *
 * A character cap belongs to the field, but it is only readable beside the value it constrains; and
 * a radio's alternatives are listed when the chosen answer does not already spell them all out, so
 * the console's own wording for the other options is on the page without repeating the chosen one.
 */
function valueCell(field) {
  // A row the pack does not answer is work rather than a value. `ownerDecision` marks a value only
  // the owner can choose (a batch size, the day a promotion is created, the address of the license
  // tester), and the walkthrough is the same guide in Russian: printing one session's number here
  // would be that session's answer offered as this app's, which is the defect the marker exists
  // against. `store-kit.py` prints the same refusal in the paste sheet, from the same model.
  if (field.ownerDecision) {
    const reason = String(field.ownerDecisionRu ?? '').trim()
    if (!reason) {
      throw new Error(`console-form.json: field ${field.id} is the owner's own decision and states no reason`)
    }
    return `TODO-HUMAN: «${fieldName(field)}» — ${reason}`
  }
  let text = String(field.valueRu ?? '')
  if (field.cap) text += ` — лимит ${field.cap}`
  const options = Array.isArray(field.optionsRu) ? field.optionsRu : []
  if (options.length > 1 && !options.every((option) => text.includes(option))) {
    text += ` · варианты: ${options.map((option) => `\`${option}\``).join(' / ')}`
  }
  // A row whose answer was read on one branch of a product form says which branch. The
  // walkthrough has no slot to resolve, so it states the branch the session observed instead of
  // printing the value as if every slot walked the same page; `store-kit.py` refuses the same row
  // for a slot on another branch and names the field.
  if (field.productKindBound) {
    const branch = resolvePath('billing.productForm.productKind.observedShortRu')
    if (!branch) throw new Error(`console-form.json: field ${field.id} is productKindBound and the model states no branch`)
    text += ` · ветка: ${branch}`
  }
  return text
}

/**
 * Why this value, which may be a rule shared with another field.
 *
 * The tag alphabet and the icon's format are stated once in the model and named from each field that
 * shows them (`whyRuFrom`), so a field row cannot drift into a second copy of the rule.
 */
function whyCell(field) {
  if (field.whyRuFrom) return String(resolvePath(field.whyRuFrom) ?? field.whyRu ?? '')
  return String(field.whyRu ?? '')
}

/** The line that says what saves this screen, and whether the console's own wording was observed. */
function actionLine(entry) {
  if (!entry.actionRu) return null
  const label = `\`${entry.actionRu}\``
  // A screen whose press belongs to the owner prints the refusal instead of the instruction: the sheet's
  // action line is what a reader acts on, and a screen that invites the press here and forbids it lower
  // has two answers to one question. The reason is the model's, never this renderer's.
  if (entry.ownerAction) {
    const reason = String(entry.ownerActionRu ?? '').trim()
    if (!reason) {
      throw new Error(
        `console-form.json: ${entry.id} marks its action the owner's and states no reason to leave it alone`
      )
    }
    return `**Действие владельца:** ${label} — ${reason}`
  }
  if (entry.actionObserved === false) {
    return `**Действие:** ${label} — метка кнопки NEEDS-VERIFICATION.`
  }
  return `**Действие:** ${label}.`
}

/** One screen (or sub-screen): its table, the action, its notes, its ladder of provenance. */
function screenMarkdown(entry, level) {
  const out = []
  const heading = typeof entry.order === 'number' ? `${entry.order}. ${entry.titleRu}` : entry.titleRu
  out.push(`${'#'.repeat(level)} ${heading}`)
  out.push('')
  if (entry.whenRu) out.push(`Только для: ${entry.whenRu}.`, '')
  if (entry.pagePathRu) {
    const page = entry.pagePathObserved
      ? `\`${entry.pagePathRu}\` (\`${entry.pagePathObserved}\`)`
      : `\`${entry.pagePathRu}\``
    out.push(`Страница: ${page}.`, '')
  }
  if (Array.isArray(entry.stripRu) && entry.stripRu.length) {
    out.push(`Шаги экрана: ${entry.stripRu.map((step) => `\`${step}\``).join(' → ')}.`, '')
  }
  // A screen whose first column is not the console's verbatim row says so above its own table: the
  // column is headed "the field, as the console shows it", and a gloss presented under that heading
  // would be a row the human cannot find. The marker is the model's, so the sentence cannot be
  // dropped by editing this renderer.
  if (entry.nameNotQuoted) {
    out.push(`**Подпись строки здесь не дословная:** ${entry.nameNotQuotedRu || ''}`, '')
  }
  // A screen whose rows were read on one branch of a product form says so above its table. The
  // walkthrough is factory-wide and has no slot, so the branch cannot be resolved here - what it can
  // do is refuse to present the rows as every slot's, and `store/console-answers.md` prints
  // `TODO-HUMAN` naming the field for a slot on another branch.
  if (entry.productKindGate) {
    const branch = resolvePath(entry.productKindNoteFrom || '')
    if (!branch) {
      throw new Error(`console-form.json: ${entry.id} carries productKindGate and names no branch note`)
    }
    out.push(`**Ветка формы:** ${branch}`, '')
  }
  // A screen whose rows exist only for a branch the *slot* chooses says so above its table, and prints
  // the sentence such a slot carries until it chooses. The walkthrough is factory-wide and has no
  // slot, so it cannot pick the branch: it reads the model's own rule and the refusal template
  // (`slotChoiceFrom` names both paths) and fills the template the same way `store-kit.py` fills it -
  // the field from this screen's own `slotKey`, the branches from the dictionary. So the guide and the
  // sheet cannot disagree, and neither can present one slot's audience as every slot's.
  if (entry.slotChoiceFrom) {
    const { rule, refusal, dictionary } = entry.slotChoiceFrom
    for (const path of [rule, refusal, dictionary]) {
      if (!path || resolvePath(path) === undefined) {
        throw new Error(`console-form.json: ${entry.id} names the missing model path ${path}`)
      }
    }
    const field = String(entry.slotKey ?? '').trim()
    if (!field) {
      throw new Error(`console-form.json: ${entry.id} carries slotChoiceFrom and names no slotKey`)
    }
    const template = String(resolvePath(refusal))
    for (const placeholder of ['{field}', '{branches}']) {
      if (!template.includes(placeholder)) {
        throw new Error(
          `console-form.json: ${refusal} carries no ${placeholder}, so the guide cannot print the sentence a slot without an audience gets`
        )
      }
    }
    const branches = Object.keys(resolvePath(dictionary) || {}).join(', ')
    out.push(String(resolvePath(rule)), '')
    out.push(`> ${template.replaceAll('{branches}', branches).replaceAll('{field}', field)}`, '')
  }
  const fields = fieldsOf(entry)
  if (fields.length) {
    out.push('| Поле (как в консоли) | Что ввести | Почему |')
    out.push('|---|---|---|')
    for (const field of fields) {
      out.push(`| ${cell(fieldName(field))} | ${cell(valueCell(field))} | ${cell(whyCell(field))} |`)
    }
    out.push('')
  }
  const action = actionLine(entry)
  if (action) out.push(action, '')
  for (const note of entry.notesRu || []) out.push(note, '')
  if (entry.needsVerification) {
    out.push(
      `**NEEDS-VERIFICATION:** ${entry.needsVerificationRu || 'источник не найден; проверьте формулировку в консоли перед отправкой.'}`,
      ''
    )
  }
  if (entry.citation) out.push(`Источник: ${entry.citation}.`, '')
  for (const sub of entry.subscreens || []) {
    out.push(screenMarkdown(sub, level + 1))
  }
  return out.join('\n')
}

/**
 * The sentence the model carries for a branch whose console greys a wizard step out.
 *
 * `{steps}` is filled from the branch the model names in `targetAudience.skippedStepsNoteBranch`: the
 * walkthrough is factory-wide and has no slot to resolve, so it shows the branch the console was
 * observed on, and `store-kit.py` renders the same sentence for whichever branch a slot declares. An
 * inactive `Шаг 2` tab read as work left undone exactly because nothing said a skipped tab is *meant*
 * to be inert - tabs carrying `aria-disabled="true"` and `is-disabled`, with `Далее` on step 1 opening
 * `Шаг 5 из 5: Сводка` directly - rather than a step the human failed to complete.
 */
function skippedStepsNote(lang) {
  const table = model.targetAudience || {}
  const template = String(table[`skippedStepsNote${lang}`] ?? '')
  if (!template) return ''
  const branch = (table.audiences || {})[table.skippedStepsNoteBranch] || {}
  const titles = new Map((table.steps || []).map((step) => [step.id, step[`title${lang}`] || step.titleRu]))
  const steps = (branch.skippedSteps || []).map((id) => `«${titles.get(id) || id}»`)
  return template.replace('{steps}', steps.join(', '))
}

/**
 * The age-group wording the publishing-overview counter prints, and whose branch it is.
 *
 * That row names the age group in *its own branch's words* (`Целевая возрастная группа: 18 и старше.`
 * on `adults-only`), so the wording belongs to a branch and not to the console. The walkthrough is
 * factory-wide and has no slot to resolve, so it shows the wording of the branch the session observed
 * and says which branch that is - the same refusal the product-form rows carry, and for the same
 * reason: printing one app's row as every app's is the defect. `store-kit.py` prints the wording of
 * whichever branch a slot declares, or `TODO-HUMAN` naming the branch nobody has read.
 *
 * The branch is named *beside* the quoted row, never inside it: the counter's `shows` is what the
 * console printed, so an annotation between its backticks is a string a human cannot find on the
 * screen - the same rule the field labels follow.
 */
function audienceStoreRowCell() {
  const branches = Object.entries((model.targetAudience || {}).audiences || {})
  const observed = branches
    .map(([id, branch]) => [id, String((branch || {}).storeRowAgeRu ?? '')])
    .filter(([, wording]) => wording)
  if (observed.length === 0) return { wording: 'TODO-HUMAN', branchId: '', others: [], work: true }
  const [branchId, wording] = observed[0]
  return { wording, branchId, others: observed.slice(1).map(([id]) => id), work: false }
}

/**
 * How completion is read for a multi-step declaration, printed once above the screens.
 *
 * The strip at the top of a wizard (`Шаг 1 из 5 … Шаг 5 из 5`) is a map of the wizard and not a
 * progress counter, and the owner read it the other way - "one item closed out of five" - concluding
 * that a declaration which was already saved was unfinished. Both the counters and the warning are the
 * model's; this function only arranges them, and a model that carries no counters is a render failure
 * rather than a document that quietly omits the paragraph the misreading proved was missing.
 *
 * Two of those counters print slot values rather than shapes, and both are handled here: the numbers
 * are the app's own and are read off the screen (`numbersRuleRu` says so), and the publishing-overview
 * row names the age group in its branch's words.
 */
function completionSection() {
  const section = model.declarationCompletion
  if (!section || !Array.isArray(section.counters) || section.counters.length === 0) {
    throw new Error(
      'console-form.json: declarationCompletion.counters must name the counters that report a finished declaration'
    )
  }
  for (const key of ['numbersRuleRu', 'audienceStoreRowRuleRu', 'warningRu']) {
    if (!String(section[key] ?? '')) {
      throw new Error(`console-form.json: declarationCompletion.${key} is the wording this document prints; it is empty`)
    }
  }
  const audience = audienceStoreRowCell()
  const out = [`## ${section.titleRu}`, '', String(section.leadRu ?? ''), '']
  for (const counter of section.counters) {
    const raw = String(counter.shows ?? '')
    const shows = raw.replace('{audienceStoreRow}', audience.wording)
    // The branch that supplied the wording is named *outside* the backticks: everything between them
    // is what the console printed, and this document's own annotation inside a quoted row is a string
    // a human cannot find on the screen.
    const branchNote =
      raw.includes('{audienceStoreRow}') && audience.branchId
        ? ` (ветка \`${audience.branchId}\`${audience.others.length ? `; подпись читалась и для веток ${audience.others.map((id) => `\`${id}\``).join(', ')}` : ''})`
        : ''
    out.push(`- **${counter.whereRu}** → ${shows}${branchNote} — ${counter.meansRu}`)
  }
  out.push('', String(section.numbersRuleRu), '', String(section.audienceStoreRowRuleRu), '')
  if (audience.work) {
    out.push(
      '**TODO-HUMAN:** подпись возрастной группы в строке «Обзор публикации» для этой ветки не читалась: ' +
        'её печатает генератор только для той ветки, которую объявил слот, а ветки без наблюдения называют себя.',
      ''
    )
  }
  out.push(`**${section.warningRu}**`)
  const note = skippedStepsNote('Ru')
  if (note) out.push('', note)
  if (section.citation) out.push('', `Источник: ${section.citation}.`)
  return out.join('\n')
}

/**
 * Why a screen id looks like the console's English label while its title is Russian.
 *
 * A declaration screen is named by the console's own English label (`App access` → `declaration-app-
 * access`) and one 2026-09-29 file named itself that way, so the model follows the same rule. The
 * paragraph exists because the two names beside each heading otherwise look like an inconsistency: the
 * id is what a file observation must use and the Russian label is what a human looks for on screen.
 * A model without the paragraph renders no section rather than an empty heading.
 */
function screenNamingSection() {
  const section = model.screenNaming
  if (!section || !String(section.ruleRu ?? '')) return ''
  return [section.titleRu ? `## ${section.titleRu}` : '', '', String(section.ruleRu)].join('\n')
}

/**
 * The items that are not a row of any reachable page, stated as a refusal rather than as screens.
 *
 * `submit-dialog` is the finding this section exists for: the dialog the submit button opens, which no
 * reader can reach while the dashboard holds a store-side row open or no production release exists, and
 * whose own rows therefore nobody has read. The two first-publication certifications used to be part of
 * it and are not any more: the 21:52 reading of 2026-09-29 found them in the dashboard's `Заявки` dialog,
 * where they are documented row by row (`declarations-dialog`). The model refuses to invent the remaining
 * one, so the document prints where the search happened, what the console showed instead and what has to
 * happen first. A finding with no blocker or no searched page is a render failure: a refusal nobody can
 * check is indistinguishable from an item that was never looked for.
 */
function unreachableSection() {
  const section = model.unreachable
  if (!section || !Array.isArray(section.items) || section.items.length === 0) return ''
  for (const key of ['titleRu', 'ruleRu']) {
    if (!String(section[key] ?? '')) {
      throw new Error(`console-form.json: unreachable.${key} is the wording this document prints; it is empty`)
    }
  }
  const out = [`## ${section.titleRu}`, '', String(section.ruleRu), '']
  for (const item of section.items) {
    for (const key of ['titleRu', 'blockerRu', 'nextActionRu']) {
      if (!String(item[key] ?? '')) {
        throw new Error(
          `console-form.json: unreachable.items[${item.id}].${key} is required: a refusal has to name the item, why it could not be read and what to do instead`
        )
      }
    }
    const searched = (item.searchedRu || []).filter(Boolean)
    if (searched.length === 0) {
      throw new Error(
        `console-form.json: unreachable.items[${item.id}].searchedRu must list the pages that were read: where the search happened is the only evidence that it happened`
      )
    }
    out.push(`### ${item.titleRu}`, '')
    for (const name of item.namesRu || []) out.push(`- ${name}`)
    if (item.namesRu?.length) out.push('')
    if (String(item.namesNotReadRu ?? '')) out.push(String(item.namesNotReadRu), '')
    out.push(`**Почему прочитать нельзя:** ${item.blockerRu}`, '')
    out.push('**Где искали:**', '')
    out.push(...searched.map((row) => `- ${row}`), '')
    out.push(`**Что делать:** ${item.nextActionRu}`, '')
    if (item.citation) out.push(`Источник: ${item.citation}.`, '')
  }
  return out.join('\n').trimEnd()
}

function render() {
  const parts = []
  parts.push('# Первый выпуск в Play Console — экраны и поля')
  parts.push('')
  parts.push(
    `> Сгенерированный документ: правьте \`${model.model}\`, а не этот файл. Рендер — \`node scripts/console-walkthrough.mjs\`; ` +
      'проверка на расхождение — `node scripts/console-walkthrough.mjs --check`.'
  )
  parts.push('')
  parts.push(`Наблюдение: ${model.observed}.`)
  parts.push('')
  parts.push(model.observedNote)
  parts.push('')
  parts.push((model.introRu || []).filter(Boolean).join('\n'))
  parts.push('')
  parts.push(completionSection())
  const naming = screenNamingSection()
  if (naming) {
    parts.push('')
    parts.push(naming)
  }
  for (const entry of [...model.screens].sort((a, b) => a.order - b.order)) {
    parts.push('')
    parts.push(screenMarkdown(entry, 2))
  }
  const unreachable = unreachableSection()
  if (unreachable) {
    parts.push('')
    parts.push(unreachable)
  }
  return `${parts.join('\n').replace(/\n{3,}/g, '\n\n').trimEnd()}\n`
}

const rendered = render()

if (CHECK) {
  let current = ''
  try {
    current = readFileSync(DOC_PATH, 'utf8')
  } catch {
    console.log(`FAIL ${model.docPath} — absent; run node scripts/console-walkthrough.mjs`)
    process.exit(1)
  }
  if (current === rendered) {
    console.log(`PASS ${model.docPath} — matches scripts/lib/console-form.json`)
    process.exit(0)
  }
  const expected = rendered.split('\n')
  const actual = current.split('\n')
  const limit = Math.max(expected.length, actual.length)
  const drift = []
  for (let index = 0; index < limit; index += 1) {
    if (expected[index] !== actual[index]) {
      drift.push(`line ${index + 1}: model=${JSON.stringify(expected[index] ?? null)} file=${JSON.stringify(actual[index] ?? null)}`)
      if (drift.length === 5) break
    }
  }
  for (const row of drift) console.log(`FAIL ${model.docPath} — ${row}`)
  console.log(
    `FAIL ${model.docPath} — drifted from scripts/lib/console-form.json; run node scripts/console-walkthrough.mjs`
  )
  process.exit(1)
}

writeFileSync(DOC_PATH, rendered, 'utf8')
console.log(`PASS ${model.docPath} — written from scripts/lib/console-form.json (${rendered.length} chars)`)
