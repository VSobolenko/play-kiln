# Signing — where the key lives, how it is made, and the one thing that must not be lost

This is the runbook a builder reads when a slot is ready to sign. It exists because the factory
used to answer the question two ways — the code put keys outside the repository, and the
`keystore.properties.example` template told a human to create `upload.jks` *inside* the slot — so a
builder had to stop and ask the owner. Nothing here is a policy of its own: the binding boundary is
`.cursor/rules/signing-keys.mdc`, and the two gates it cites are `.cursor/rules/authorization.mdc`
and `.cursor/rules/release-gate.mdc`.

## Where the key lives, and why there

Every slot's upload key is a single file:

```text
<home>/.playkiln/keystores/<applicationId>.jks
```

On this machine that is `C:/Users/vsobo/.playkiln/keystores/`, and the file name is the slot's
`applicationId` (for `com.gexetr.teachergradebook`, `com.gexetr.teachergradebook.jks`).

The path is decided in **one** place. `scripts/lib/signing.mjs` owns the keystore location, the
`upload` alias, the RSA key size, the validity period and the exact `keystore.properties.example`
text; `scripts/sign-release.mjs` generates and reads the key from there, and `scripts/core/signing.mjs`
is the check that a template cannot drift from it. **When a value has to change, change it in the
module** — never in a template, a document, or a second script — so the generator, the checker and
every example stay one answer.

Why the key is deliberately *outside* the repository:

- The project is data. It can be re-cloned, deleted, archived, or absent — `проект может чисто
  случайно отсутствовать`. The upload key is not data: it is the app's permanent identity, and it
  cannot be regenerated from the sources. A key kept inside the tree is lost with the tree.
- The configuration, by contrast, *should* travel with the slot: `keystore.properties` is
  per-slot and gitignored, so it follows the working copy while pointing at the outside key. That
  split — configuration in the project, key outside it — is the owner's arrangement, and this
  runbook is its human half.

## What is in `keystore.properties`, and why it is gitignored

`apps/<slot>/keystore.properties` is the per-slot pointer to the outside key. It carries exactly four
properties:

| Property | What it is |
|---|---|
| `storeFile` | the absolute path to the `.jks` in `~/.playkiln/keystores/` |
| `storePassword` | the keystore password |
| `keyAlias` | the key alias — `upload` |
| `keyPassword` | the key password |

It is gitignored at two levels. The root `.gitignore` ignores every key file (`*.jks`, `*.keystore`);
the slot's own `.gitignore`, copied from `scaffold/android-compose/.gitignore`, additionally ignores
`keystore.properties`. It is ignored because it holds a **password**: the store and key passwords are
the same machine-generated value, and a repository is the wrong place for either. The config is
machine-local by design, and `scripts/lib/release-approval.mjs` treats it as such — `keystore.properties`
is excluded from the approval digest, because its bytes differ between two machines that build the
same revision (the home path differs), and hashing it would make one revision have one digest per
machine.

`apps/2026-09-26-teacher-gradebook/keystore.properties` is a live example of the shape. Its
`storeFile` points at `C:/Users/vsobo/.playkiln/keystores/com.gexetr.teachergradebook.jks`; the
passwords in it are its own machine-generated values and are never quoted, printed or copied
anywhere.

## How a key is created

Two commands, and the first is the one a builder runs before anything else.

**Inspect first.** If a key and config already exist, there is nothing to create:

```bash
node scripts/sign-release.mjs --slot <slug>
```

With a config present it prints the alias and the upload-certificate SHA-256; with no
`keystore.properties` it prints `BLOCKED` and the `keytool` shape it expected, and exits 2.

**Generate, and only with the owner's approval already recorded.**

```bash
node scripts/sign-release.mjs --slot <slug> --generate
```

`--generate` **refuses to run without the owner's recorded release approval**
(`checkReleaseApproval` in `scripts/sign-release.mjs`). That refusal is correct and must not be
weakened: generating a key is the one irreversible step in the factory — it mints the app's permanent
identity together with the password that unlocks it — so the approval that `.cursor/rules/release-gate.mdc`
requires for release work is required here too, and `.cursor/rules/authorization.mdc` is why an agent
may not create one on its own initiative. If the command refuses, the answer is to ask the owner for
the approval, never to work around the gate.

When it runs, it:

- creates `~/.playkiln/keystores/` with owner-only permissions if it does not exist, and **refuses to
  overwrite an existing keystore**;
- generates the password itself — 24 random bytes, base64url, trimmed to 32 characters — so the
  password is **machine-generated and is not a value anyone chose or memorised**;
- generates an RSA 4096 key, alias `upload`, validity 10000 days, distinguished name
  `CN=<applicationId>, OU=Upload, O=PlayKiln, C=US`;
- writes `keystore.properties` (owner-only) containing `storeFile`, `storePassword`, `keyAlias`,
  `keyPassword`, and prints the upload certificate's SHA-256.

**Because the password is machine-generated, it exists only in the gitignored `keystore.properties`.**
A `.jks` without its password is a random file. That is why the backup below is not a nicety: what has
to be captured is the **key and the password together**, and it has to be captured before the file
that holds the password is lost.

## The upload key versus Google's app-signing key

The factory creates and holds the **upload key**. When the app is enrolled in Play App Signing,
Google holds the real **app-signing key** and re-signs the artifact the factory uploads, so the factory
proves its identity to Console rather than to the device.

Two facts about the pair are recorded in this repository, in the `keystore.properties.example` text
itself: *Play App Signing keeps the real app-signing key at Google*, and *if the upload key is lost or
compromised, request an upload-key reset through Play Console*.

The wider distinction — that a **lost upload key is resettable through Console, while a lost
app-signing key is not** — is **NEEDS-VERIFICATION**, exactly as
`docs/play-publishing-playbook.md` §4 states. No source for it exists in this repository, so it is
recorded here as unverified rather than repeated as settled fact. What this runbook can say without a
source is the consequence of the factory's own arrangement: the factory only ever holds the upload key,
so the durable object *it* must back up is the upload key.

## How to inspect what exists and its fingerprint

```bash
# what the factory would sign with for this slot, and its fingerprint
node scripts/sign-release.mjs --slot <slug>

# every outside key file, newest last
ls ~/.playkiln/keystores/

# which slot points at which key (prints no secret — only the storeFile line)
rg -n "^storeFile=" apps/ --glob "keystore.properties"
```

### Recorded state, 2026-10-02 (MEASURED)

Read by the factory on 2026-10-02, so a later reader can tell what has changed rather than assuming:

| Key file in `<home>/.playkiln/keystores/` | Slot that points at it | Verdict |
|---|---|---|
| `com.gexetr.smoke.jks` | `apps/2026-09-22-smoke` | matched |
| `com.gexetr.pipeproof.jks` | `apps/2026-09-23-pipe-proof` | matched |
| `com.gexetr.pipesizing.jks` | `apps/2026-09-25-pipe-sizing` | matched; slot killed |
| `com.gexetr.teachergradebook.jks` | `apps/2026-09-26-teacher-gradebook` | matched; slot published |
| `com.gexetr.lawnroundlog.jks` | `apps/2026-10-01-lawn-round-log` | matched |
| `com.playkiln.smoke.jks` | **none** | **ORPHAN** — namespace `scripts/lib/namespaces.mjs` forbids |

Two absences, both intentional and neither a defect to fix here:

- **`apps/2026-10-02-laneset` has no key and no config.** That is correct until the owner authorises
  one; `node scripts/sign-release.mjs --slot laneset` refuses and names this runbook. The owner's
  decision on 2026-10-02 was to leave it that way.
- **No backup of the directory is recorded anywhere.** The owner's decision on 2026-10-02 was to keep
  the arrangement as it is and treat § "The backup" as his own act, and the orphan above was reported
  rather than deleted, because discarding key material is not the factory's decision.

`~/.android/debug.keystore` is the Android debug keystore and is not part of this system.

`scripts/prepare-release.mjs` verifies the built release APK's certificate against the configured
upload certificate and records that fingerprint in `dist/release-manifest.json` as
`signing.fingerprintSha256` (with `signing.alias`). The human half is the comparison
`docs/play-publishing-playbook.md` §4 names: compare the factory's printed/recorded SHA-256 with the
expected upload certificate in Play Console before an upload.

## What is lost if the directory is lost

The `~/.playkiln/keystores/` directory holds the slot's signing identity, and nothing can rebuild it:

- the sources can be rebuilt and the app can be recompiled, but a signature cannot be regenerated —
  a new key is a **different** app identity, not a repaired one;
- with Play App Signing, an upload key is the factory's proof of identity for uploads, and losing it
  means no further upload under that key until Console resets it;
- if a slot's key is not an upload key but the signing key itself, losing it ends the app's ability to
  be updated at all.

No gate can see this. Nothing in the factory reads `~/.playkiln/keystores/`, no check can tell a
backed-up key from an unbacked-up one, and the loss is invisible until the next release tries to sign.
The directory being outside the repository is what makes it safe from a `git clean` and, at the same
time, what makes it easy to forget.

## The backup

This is the step the whole arrangement points at: the configuration travels with the project, the key
does not, so the key must be copied somewhere durable that survives the project *and* the machine.

Back up, together:

- the `.jks` file — the whole `~/.playkiln/keystores/` directory keeps every slot's key in one place,
  or back up the single `<applicationId>.jks` for the slot at hand;
- its password. The store and key passwords are the same machine-generated value, and they live only
  in the slot's gitignored `keystore.properties`. **A `.jks` without its password is unusable**, so a
  backup that copies the file and not the password has backed up nothing.

Store the copy off this machine — wherever the owner keeps durable material (a password manager, an
encrypted archive, an offline copy). Then confirm the copy can be read back, because an unreadable
backup is the same as none.

**The honest limit.** No program in this factory can prove a backup was made. A check can read
`~/.playkiln/keystores/` and the slot's config; it cannot see a copy that lives somewhere it is not
allowed to read, and "there is a backup" is not a fact any file here can assert. The backup is the
owner's act, and this section exists so the builder tells him instead of assuming it.

## Rotation

Rotation is an owner's decision, not a maintenance step, and this runbook does **not** invent the
procedure. The reference is `docs/play-publishing-playbook.md` §4, which carries the reset/rotation
steps and preserves their **NEEDS-VERIFICATION** labels. The boundary is unchanged from creation:
`.cursor/rules/authorization.mdc` requires the owner's explicit instruction in the current
conversation for generating, rotating or overwriting a signing credential, and the release approval
that guards `--generate` guards a replacement key the same way. A rotation that is not asked for is not
a rotation; it is the violation `.cursor/rules/signing-keys.mdc` exists to prevent.
