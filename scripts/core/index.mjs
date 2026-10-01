// The check registry. One entry per fact, each a function the entry point calls in-process.
//
// A check is never a script that shells out to another script: the measured defect this replaces was
// a chain (verify.mjs ran selftest.mjs, which discovered and ran 22 suites, some of which
// asserted that other scripts called runGradleTasks). That chain is why a one-line fix cost an hour
// and why a green run could not be read as anything.
import { checkAssetFixtures, checkLimitsAgreement, checkReleaseNoteFixtures, packChecks } from './assets.mjs'
import { apkChecks, sixteenKbFixtures } from './apk.mjs'
import { buildChecks, buildFixtures, checkCopyFilter, checkNamespaceSweep, checkScratchHygiene, checkToolchainPins } from './build.mjs'
import { checkConfig } from './config.mjs'
import { checkFactLedger, factFixtures } from './facts.mjs'
import { checkAdaptiveIcon, iconFixtures } from './icon.mjs'
import { harnessChecks, harnessFixtures } from './harness.mjs'
import { manifestChecks, manifestFixtures } from './manifest.mjs'
import { policyChecks, policyFixtures } from './policy.mjs'
import { checkHarnessRefFixtures, checkHarnessRefs, checkSecretFixtures, checkSize, slotChecks } from './project.mjs'

import { localizationChecks, localizationFixtures, localizationProjectChecks } from './localization.mjs'
import { settingsChecks, settingsFixtures, settingsProjectChecks } from './settings.mjs'
import { uiuxChecks, uiuxFixtures, uiuxProjectChecks } from './uiux.mjs'

/**
 * A fixture suite returns either one verdict or a list of them, and the three UI baselines grew
 * different shapes. Normalising here rather than at each call site is deliberate: a suite that
 * returned a bare verdict would otherwise be spread as zero rows and vanish from the report, which
 * is the "a check that does not run cannot be told apart from a check that does not exist" failure
 * `lib.mjs` names.
 */
const asVerdicts = (value) => (Array.isArray(value) ? value : [value])

/**
 * Every check, in report order.
 *
 * `needsSlot` marks a check that requires a slot to inspect. A run without `--slot` still evaluates
 * every project-wide invariant and reports the slot checks as SKIP — never as absent, because a
 * check that silently does not run cannot be told apart from a check that does not exist.
 */
export const CHECKS = Object.freeze([
  {
    id: 'invariants',
    needsSlot: false,
    run: () => [
      checkLimitsAgreement(),
      checkAssetFixtures(),
      checkReleaseNoteFixtures(),
      iconFixtures(),
      sixteenKbFixtures(),
      buildFixtures(),
      factFixtures(),
      harnessFixtures(),
      manifestFixtures(),
      policyFixtures(),
      checkHarnessRefFixtures(),
      checkHarnessRefs(),
      checkSecretFixtures(),
      checkToolchainPins(),
      checkNamespaceSweep(),
      checkCopyFilter(),
      checkScratchHygiene(),
      checkFactLedger(),
      ...harnessChecks(),
      ...asVerdicts(uiuxFixtures()),
      ...uiuxProjectChecks(),
      ...asVerdicts(settingsFixtures()),
      ...settingsProjectChecks(),
      ...asVerdicts(localizationFixtures()),
      ...localizationProjectChecks(),
      checkSize(),
      ...checkConfig()
    ]
  },
  { id: 'assets', needsSlot: true, run: (slotDir) => packChecks(slotDir) },
  { id: 'icon', needsSlot: true, run: (slotDir) => [checkAdaptiveIcon(slotDir)] },
  { id: 'apk', needsSlot: true, run: (slotDir) => apkChecks(slotDir) },
  { id: 'build', needsSlot: true, run: (slotDir) => buildChecks(slotDir) },
  { id: 'manifest', needsSlot: true, run: (slotDir) => manifestChecks(slotDir) },
  { id: 'policy', needsSlot: true, run: (slotDir) => policyChecks(slotDir) },
  { id: 'project', needsSlot: true, run: (slotDir) => slotChecks(slotDir) },
  // The UI baseline, added 2026-09-30 after the owner's review of the first live app. Slot-scoped,
  // and honest about applicability: `baseline.mjs` turns a finding into a FAIL only for a slot the
  // baseline gates, and prints it as a note everywhere else rather than hiding it.
  {
    id: 'ui',
    needsSlot: true,
    run: (slotDir) => [...uiuxChecks(slotDir), ...settingsChecks(slotDir), ...localizationChecks(slotDir)]
  }
])

export const CHECK_IDS = Object.freeze(CHECKS.map((check) => check.id))
