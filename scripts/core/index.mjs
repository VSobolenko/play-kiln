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
import { ICON_IDS, iconsChecks, iconsFixtures } from './icons.mjs'
import { groundedClaimsFixtures, harnessChecks, harnessFixtures, rulesFixtures } from './harness.mjs'
import { manifestChecks, manifestFixtures } from './manifest.mjs'
import { deviceChecks, deviceFixtures } from './device.mjs'
import { namingChecks, namingFixtures } from './naming.mjs'
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
 * The icon legs, declared literally because `checkIdsFromRegistry` reads the registry as text and a
 * fact can therefore only name an id that appears here as a literal `id:`. `ICON_IDS` stays the
 * sensor's own list; the assertion below turns a drift between the two into a load-time failure
 * rather than a fact that quietly stops being enforced.
 */
const ICON_REPORTS = [
  { id: 'icons.mark' },
  { id: 'icons.launcher' },
  { id: 'icons.provenance' },
  { id: 'icons.licence' },
  { id: 'icons.offline' },
  { id: 'icons.deprecated' },
  { id: 'icons.tint' }
]
if (ICON_REPORTS.map((report) => report.id).join() !== ICON_IDS.join()) {
  throw new Error('the icon group reports list has drifted from ICON_IDS in scripts/core/icons.mjs')
}

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
      ...asVerdicts(iconsFixtures()),
      sixteenKbFixtures(),
      buildFixtures(),
      factFixtures(),
      harnessFixtures(),
      manifestFixtures(),
      policyFixtures(),
      ...asVerdicts(namingFixtures()),
      ...asVerdicts(deviceFixtures()),
      checkHarnessRefFixtures(),
      checkHarnessRefs(),
      rulesFixtures(),
      groundedClaimsFixtures(),
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
  {
    // Iconography, both halves of the split the owner asked for: the launcher mark a slot owns
    // outright, and the UI-icon sets it borrows from a licence-cleared library.
    //
    // `reports` is written out literally rather than derived from the sensor's `ICON_IDS`, and that
    // is deliberate: `checkIdsFromRegistry` reads this file as *text*, so a fact may only name a leg
    // whose id appears here as a literal `id:`. Deriving it would look like one source of truth and
    // would put every leg out of the ledger's reach. The assertion beside `ICON_REPORTS` is what
    // stops the two lists drifting, because this duplication is the price of being nameable.
    id: 'icon',
    needsSlot: true,
    reports: ICON_REPORTS,
    run: (slotDir) => [checkAdaptiveIcon(slotDir), ...iconsChecks(slotDir)]
  },
  { id: 'apk', needsSlot: true, run: (slotDir) => apkChecks(slotDir) },
  { id: 'build', needsSlot: true, run: (slotDir) => buildChecks(slotDir) },
  { id: 'manifest', needsSlot: true, run: (slotDir) => manifestChecks(slotDir) },
  // `slot.naming` sits with `slot.differentiators`: both are decisions the slot recorded before it
  // was built, and both are read the same way — the contract on disk against the pack it produced.
  // `slot.device` sits here for the same reason: it reads a record the agent wrote about its own run
  // on the phone, so "I tested it on the device" is a claim that resolves to a file rather than prose.
  {
    id: 'policy',
    needsSlot: true,
    run: (slotDir) => [...policyChecks(slotDir), ...namingChecks(slotDir), ...deviceChecks(slotDir)]
  },
  { id: 'project', needsSlot: true, run: (slotDir) => slotChecks(slotDir) },
  // The UI baseline, added 2026-09-30 after the owner's review of the first live app. Slot-scoped,
  // and honest about applicability: `baseline.mjs` turns a finding into a FAIL only for a slot the
  // baseline gates, and prints it as a note everywhere else rather than hiding it.
  {
    id: 'ui',
    needsSlot: true,
    // The ids this group reports inside its rows. A fact may legitimately be enforced by one sensor
    // rather than by the group as a whole — "edge-to-edge is mandatory at targetSdk" is `uiux.insets`
    // and not `ui` — and `checkIdsFromRegistry` reads every `id:` in this file, so a reported id is
    // declared here for the same reason the group's own id is: the ledger may name a function that
    // really runs, and it is this entry that runs it. `uiux.platform` joined on 2026-10-02.
    reports: [{ id: 'uiux.insets' }, { id: 'uiux.platform' }],
    run: (slotDir) => [...uiuxChecks(slotDir), ...settingsChecks(slotDir), ...localizationChecks(slotDir)]
  }
])

export const CHECK_IDS = Object.freeze(CHECKS.map((check) => check.id))
