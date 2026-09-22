/**
 * Published namespace for every PlayKiln slot — single source of truth.
 *
 * The developer account publishes apps as `com.gexetr.<slug>` (see
 * `.cursor/rules/project-conventions.mdc`). Every script that creates, validates,
 * or reports an applicationId imports from here so the rule, the message shown to
 * the user, and the scaffold package can never drift apart.
 *
 * Why this module exists: `new-slot.mjs` once hardcoded a validation regex and a
 * separate hand-written error string. When the prefix changed, the string kept the
 * old namespace and told the user to use the wrong value. Deriving both from one
 * constant removes that whole class of defect.
 */

/** Namespace owned by the developer account on Google Play. */
export const packagePrefix = 'com.gexetr'

/** Package name compiled into the scaffold before slot substitution. */
export const scaffoldPackage = `${packagePrefix}.scaffold`

/**
 * Accepted shape of a slot applicationId: the fixed prefix plus one or more
 * lowercase alphanumeric segments (no dots, no dashes, no uppercase) so that the
 * Play package name stays valid and each slot stays unique.
 */
export const applicationIdPattern = new RegExp(
  `^${packagePrefix.replaceAll('.', '\\.')}\\.[a-z0-9]+$`
)

/** Human-readable form of the rule, derived from the pattern itself. */
export const applicationIdRule = applicationIdPattern.source

/** Build the default applicationId for a slug (dashes are not valid in a package). */
export function applicationIdForSlug(slug) {
  return `${packagePrefix}.${String(slug).replaceAll('-', '')}`
}
