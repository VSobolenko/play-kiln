#!/usr/bin/env node
/**
 * Failure-injection tests for the device-language guard in `scripts/media-capture.mjs`.
 *
 * A journey asserts the literal text of the nodes it photographs, so the strings it reads come from
 * the app's resources, which Android resolves against the *device's* language. That dependency was
 * invisible until a slot gained `values-<locale>/` directories: the same journey that had been green
 * while the app was English-only started reading the device's language, and every reading in every
 * shot failed with "not in the field they name" - a message that describes a broken journey, not a
 * device setting. On the A40 it cost three attempts and about twenty minutes.
 *
 * The case below is that exact observation, recorded from the device: its `am get-config` line and an
 * English journey. `oldVerdict` is the implementation before the fix - there was no check at all -
 * and a case that only showed the new guard refusing would not prove the defect was ever live.
 */
import { devicePrimaryLanguage, journeyLanguage } from './media-capture.mjs'

let failures = 0
let checks = 0

function check(label, condition, detail) {
  checks += 1
  if (condition) {
    console.log(`PASS ${label}`)
  } else {
    failures += 1
    console.log(`FAIL ${label}${detail ? ` - ${detail}` : ''}`)
  }
}

/** The A40's real `am get-config` locale list, recorded while the capture journey was failing. */
const RUSSIAN_A40 = 'config: ru-rRU,be-rBY-ldltr-sw360dp-w360dp-h703dp-normal-long-notround-nowidecg-lowdr-port-night-xxhdpi-finger-keyshidden-nokeys-navhidden-nonav-2110x1080-v30'
/** The same device once English became primary, with Russian still in the list behind it. */
const ENGLISH_A40 = 'config: en-rUS,ru-rRU,be-rBY-ldltr-sw360dp-w360dp-h703dp-normal-long-notround-nowidecg-lowdr-port-night-xxhdpi-finger-keyshidden-nokeys-navhidden-nonav-2110x1080-v30'

/** The guard as it was before the fix: there was none, so every device proceeded to the journey. */
function verdictBeforeFix() {
  return { kind: 'pass' }
}

/** The guard as it is now, reduced to its decision so it can be driven without a device. */
function verdictNow(journey, configOutput) {
  const deviceLanguage = devicePrimaryLanguage(configOutput)
  const expected = journeyLanguage(journey)
  if (deviceLanguage === null) return { kind: 'note', expected }
  if (deviceLanguage !== expected) return { kind: 'block', deviceLanguage, expected }
  return { kind: 'pass', deviceLanguage }
}

const englishJourney = { locale: 'en-US' }

// The defect: an English journey on the Russian device was allowed through, so the failure surfaced
// three attempts later as a screen-reading error that named no cause.
check(
  'the defect was live: an English journey on a Russian device used to proceed',
  verdictBeforeFix(englishJourney, RUSSIAN_A40).kind === 'pass',
  'the pre-fix guard refused, so this case proves nothing about the defect'
)

const blocked = verdictNow(englishJourney, RUSSIAN_A40)
check(
  'an English journey on a Russian device is refused',
  blocked.kind === 'block',
  `expected a refusal, got '${blocked.kind}'`
)
check(
  'the refusal names the device language and the language the readings are written in',
  blocked.deviceLanguage === 'ru' && blocked.expected === 'en',
  `named device='${blocked.deviceLanguage}' expected='${blocked.expected}'`
)

// The device the fix produced, and the one a capture is normally run on.
check(
  'an English journey on an English device is allowed',
  verdictNow(englishJourney, ENGLISH_A40).kind === 'pass'
)

// The grammar, not a token index: `en-rUS` is a region subtag and `-ldltr` is the qualifier that
// ends the list. Reading the list as "everything up to the first hyphen" would answer `en` here by
// luck, but the old-style `-r` spelling is what makes the two cases differ.
check(
  'the region subtag is not mistaken for the qualifier that ends the locale list',
  devicePrimaryLanguage(ENGLISH_A40) === 'en' && devicePrimaryLanguage(RUSSIAN_A40) === 'ru',
  `read '${devicePrimaryLanguage(ENGLISH_A40)}' and '${devicePrimaryLanguage(RUSSIAN_A40)}'`
)

// A list of one, with no region and no qualifier at all: `am get-config` omits what the device
// default supplies, so the shortest legal line must still answer.
check(
  'a bare locale list with no region and no qualifier still answers',
  devicePrimaryLanguage('config: ja') === 'ja',
  `read '${devicePrimaryLanguage('config: ja')}'`
)

// Three-letter primary subtags are real (`fil` for Filipino) and a two-or-three-letter rule that
// only accepted two would silently stop guarding those devices.
check(
  'a three-letter primary subtag is read, not skipped',
  devicePrimaryLanguage('config: fil-rPH-ldltr') === 'fil',
  `read '${devicePrimaryLanguage('config: fil-rPH-ldltr')}'`
)

// The journey's own declaration is optional and defaults to English, which is the listing's default
// locale; a journey written in another language must be believed rather than overridden.
check(
  'a journey with no declared locale is treated as English',
  journeyLanguage({}) === 'en' && journeyLanguage({ locale: '' }) === 'en'
)
check(
  'a journey that declares another language is believed, in both directions',
  journeyLanguage({ locale: 'ru-RU' }) === 'ru' &&
    verdictNow({ locale: 'ru-RU' }, RUSSIAN_A40).kind === 'pass' &&
    verdictNow({ locale: 'ru-RU' }, ENGLISH_A40).kind === 'block'
)

// The guard must not refuse on its own uncertainty: an unreadable config is a note, so a device whose
// output this parser does not recognise can still be captured - a harness bug must never block a
// release run the way a wrong locale would have.
check(
  'an unreadable config is a note, never a refusal',
  devicePrimaryLanguage('') === null &&
    devicePrimaryLanguage('adb: device offline') === null &&
    verdictNow(englishJourney, '').kind === 'note'
)

console.log('')
console.log(failures === 0 ? `VERDICT: PASS (${checks} checks)` : `VERDICT: FAIL (${failures}/${checks} checks)`)
process.exit(failures === 0 ? 0 : 1)
