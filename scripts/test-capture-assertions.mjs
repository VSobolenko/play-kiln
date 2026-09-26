#!/usr/bin/env node
/**
 * Failure-injection tests for the screen-reading assertion in `scripts/media-capture.mjs`.
 *
 * The assertion that reads a screenshot back was field-blind: it flattened every `text=` value of a
 * `uiautomator dump` into one list and checked exact membership, so a reading typed into the wrong
 * field passed, readings with no form on screen at all passed, and a one-character entry like "4"
 * or "A" passed on almost any screen. Each case below drives the OLD reader (reproduced here) and the
 * NEW one against the same synthetic dump and asserts that the old one passes where the new one
 * refuses. A case that only showed the new reader failing would not prove the defect was ever live.
 */
import process from 'node:process'

import { assertReadings, expectedReadings, parseDump, validateExpectations } from './media-capture.mjs'

let failures = 0
let checks = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` — ${detail}` : ''}`)
  }
}

/** The assertion as it was before the fix: a flat list of every text value, exact membership. */
function oldAssertReadings(texts, expected, label) {
  if (expected.length === 0) return
  const missing = expected.filter((entry) => !texts.includes(entry))
  if (missing.length > 0) throw new Error(`${label}: expected readings absent: ${missing.join(', ')}`)
}

function oldThrows(texts, expected) {
  try {
    oldAssertReadings(texts, expected, 'old')
    return false
  } catch {
    return true
  }
}

function newThrows(nodes, expected, label = 'new') {
  try {
    assertReadings(nodes, expected, label)
    return false
  } catch {
    return true
  }
}

function captureError(fn) {
  try {
    fn()
    return null
  } catch (error) {
    return error
  }
}

function node(text, { className = 'android.view.View', resourceId = '', contentDesc = '', hint = '', bounds = null } = {}) {
  return { text, resourceId, contentDesc, hint, className, bounds }
}

function rect(left, top, right, bottom) {
  return { left, top, right, bottom }
}

function textsOf(nodes) {
  return nodes.map((entry) => entry.text).filter(Boolean)
}

/* A Compose-like form: each field's label node overlaps the text node that holds its value. */
const form = [
  node('Material or system', { bounds: rect(40, 300, 520, 360) }),
  node('Schedule 40 steel', { className: 'android.widget.EditText', bounds: rect(40, 340, 1040, 470) }),
  node('Pressure class', { bounds: rect(40, 520, 520, 580) }),
  node('0.5 psig', { className: 'android.widget.EditText', bounds: rect(40, 560, 1040, 690) })
]

/* 1. The positive control: a reading in the field its label names is accepted, so the refusal cases
 *    below cannot be passing for a reason that has nothing to do with field association. */
check(
  'the new reader accepts a reading in the field its label names',
  !newThrows(form, [{ value: '0.5 psig', field: { label: '^Pressure class$' } }])
)
check('a plain string is still accepted on an editable input (backwards compatibility)', !newThrows(form, ['0.5 psig']))
check(
  'a label merged into content-desc identifies the field without a separate label node',
  !newThrows(
    [node('0.5 psig', { className: 'android.widget.EditText', contentDesc: 'Pressure class', bounds: rect(40, 560, 1040, 690) })],
    [{ value: '0.5 psig', field: { label: '^Pressure class$' } }]
  )
)
check(
  'a label exposed as hint text identifies a Compose field without a separate label node',
  !newThrows(
    [node('0.5 psig', { className: 'android.widget.EditText', hint: 'Pressure class', bounds: rect(40, 560, 1040, 690) })],
    [{ value: '0.5 psig', field: { label: 'Pressure class' } }]
  )
)

/* 2. FAILURE 1: the same text sits in the WRONG field. The old flat reader cannot see fields at all. */
const wrongField = [
  node('Material or system', { bounds: rect(40, 300, 520, 360) }),
  node('0.5 psig', { className: 'android.widget.EditText', bounds: rect(40, 340, 1040, 470) }),
  node('Pressure class', { bounds: rect(40, 520, 520, 580) }),
  node('', { className: 'android.widget.EditText', bounds: rect(40, 560, 1040, 690) })
]
check(
  'the old flat reader passes a value typed into the wrong field',
  !oldThrows(textsOf(wrongField), ['0.5 psig'])
)
check(
  'the new reader refuses a value typed into the wrong field',
  newThrows(wrongField, [{ value: '0.5 psig', field: { label: '^Pressure class$' } }])
)

/* 3. FAILURE 2: the readings are on screen but no form is - only captions and result paragraphs. */
const noForm = [
  node('Material or system', { bounds: rect(40, 300, 520, 360) }),
  node('Total load (MBH)', { bounds: rect(40, 900, 520, 960) }),
  node('Developed length (ft)', { bounds: rect(40, 1200, 520, 1260) }),
  node('100', { bounds: rect(40, 1500, 200, 1560) }),
  node('40', { bounds: rect(240, 1500, 400, 1560) }),
  node('60.00 ft total run', { bounds: rect(40, 1800, 600, 1860) })
]
check(
  'the old flat reader passes readings present with no form on screen',
  !oldThrows(textsOf(noForm), ['100', '40'])
)
check(
  'the new reader refuses a plain string that is not an editable input',
  newThrows(noForm, ['100', '40'])
)
check(
  'the new reader refuses a field-associated reading when no field matches',
  newThrows(noForm, [{ value: '100', field: { label: '^Total load \\(MBH\\)$' } }])
)

/* 4. FAILURE 3: one-character expectations. The old reader passes the bare digit; the new one refuses
 *    it at load time, before a device is touched, and again if it reaches the assertion. */
const digitForm = [
  node('Fitting quantity', { bounds: rect(40, 300, 520, 360) }),
  node('4', { className: 'android.widget.EditText', bounds: rect(40, 340, 1040, 470) })
]
check('the old flat reader passes the bare digit "4"', !oldThrows(['Schedule 40 steel', '4'], ['4']))
check(
  'load-time validation refuses the bare digit "4"',
  captureError(() => validateExpectations(expectedReadings({ expect: ['4'] }), 'shot-02')) !== null
)
check(
  'load-time validation refuses the bare character "A"',
  captureError(() => validateExpectations(expectedReadings({ expect: ['A'] }), 'shot-03')) !== null
)
check(
  'load-time validation refuses the bare digit "2"',
  captureError(() => validateExpectations(expectedReadings({ expect: ['2'] }), 'shot-03')) !== null
)
check(
  'load-time validation accepts a two-character reading',
  captureError(() => validateExpectations(expectedReadings({ expect: ['40'] }), 'shot-02')) === null
)
check('the new reader refuses the bare digit "4" even when called directly', newThrows(digitForm, ['4']))

/* 5. Do not regress the historical corruption the flat reader did catch: IME autocorrect produced
 *    concatenated strings, so an exact membership check failed. That must still fail, both ways. */
const gibberish = [
  node('Material or system', { bounds: rect(40, 300, 520, 360) }),
  node('Schedule 40 steel to the', { className: 'android.widget.EditText', bounds: rect(40, 340, 1040, 470) }),
  node('Pressure class', { bounds: rect(40, 520, 520, 580) }),
  node('0.5 Paige 100y40v4 5 to the', { className: 'android.widget.EditText', bounds: rect(40, 560, 1040, 690) })
]
check('the old reader still catches the IME corruption', oldThrows(textsOf(gibberish), ['0.5 psig']))
check(
  'the new reader still catches the IME corruption',
  newThrows(gibberish, [{ value: '0.5 psig', field: { label: '^Pressure class$' } }])
)

/* 6. `parseDump` keeps the attributes the field check needs; a reader that drops them is the defect. */
{
  const xml = '<hierarchy rotation="0"><node index="0" text="Pressure &amp; class" resource-id="com.gexetr.pipesizing:id/pressure" class="android.widget.EditText" content-desc="Pressure class" hint="Pressure class (psig)" bounds="[40,560][1040,690]" /></hierarchy>'
  const parsed = parseDump(xml)
  check('parseDump reads the node', parsed.length === 1, `got ${parsed.length}`)
  check('parseDump decodes XML entities in text', parsed[0]?.text === 'Pressure & class', parsed[0]?.text)
  check(
    'parseDump keeps resource-id and content-desc',
    parsed[0]?.resourceId === 'com.gexetr.pipesizing:id/pressure' && parsed[0]?.contentDesc === 'Pressure class'
  )
  check('parseDump keeps hint, where Compose exposes a field label', parsed[0]?.hint === 'Pressure class (psig)', parsed[0]?.hint)
  check(
    'parseDump keeps the class that separates an input from a paragraph',
    parsed[0]?.className === 'android.widget.EditText'
  )
  check(
    'parseDump parses bounds',
    parsed[0]?.bounds?.left === 40 && parsed[0]?.bounds?.bottom === 690,
    JSON.stringify(parsed[0]?.bounds)
  )
}

/* 7. A resource-id names the field directly when a slot sets one. */
const byId = [
  node('0.5 psig', { className: 'android.widget.EditText', resourceId: 'com.gexetr.pipesizing:id/pressure', bounds: rect(40, 560, 1040, 690) })
]
check(
  'a resource-id field association accepts the right node',
  !newThrows(byId, [{ value: '0.5 psig', field: { resourceId: 'id/pressure$' } }])
)
check(
  'a resource-id field association refuses a node with a different id',
  newThrows(byId, [{ value: '0.5 psig', field: { resourceId: 'id/material$' } }])
)

/* 8. The failure has to be debuggable: it must name the shot, the value and the field it wanted. */
{
  const error = captureError(() =>
    assertReadings(wrongField, [{ value: '0.5 psig', field: { label: '^Pressure class$' } }], 'shot-01 (Enter the sizing inputs)')
  )
  check(
    'the failure names the shot, the expected value and the field it wanted',
    error !== null &&
      error.message.includes('shot-01') &&
      error.message.includes('0.5 psig') &&
      error.message.includes('Pressure class'),
    error?.message
  )
}

console.log(`\n${checks - failures}/${checks} checks passed`)
process.exit(failures === 0 ? 0 : 1)
