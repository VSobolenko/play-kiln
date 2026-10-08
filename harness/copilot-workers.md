# Copilot worker operator guide

Use GitHub Copilot CLI workers for bounded, durable engineering tasks when a second peer model
family is wanted beside Codex. Copilot is a **second worker channel** — a peer model family, not a
new role and not a replacement for Codex. The five role cards in `harness/roles/` are
channel-agnostic, so a channel runs them by handing the worker a prompt file; Copilot does not
introduce new roles. The runner keeps output in `.kiln/copilot/`, and it does not make a worker's
self-report authoritative.

The channel order is stated once, in `harness/dual-agent.md` § "Fallback ladder":
`Codex worker → GitHub Copilot CLI worker → Cursor generator subagent → orchestrator`. This guide
does not restate the trigger table; it documents how to drive the Copilot channel.

## Commands

```bash
# Start one worker. The runner stays in the foreground.
node scripts/copilot-run.mjs run --id <id> --prompt-file .kiln/copilot/prompts/<prompt>.md

# Inspect all workers or one worker.
node scripts/copilot-run.mjs list
node scripts/copilot-run.mjs status
node scripts/copilot-run.mjs status --id <id>

# Read the final answer or recent logs.
node scripts/copilot-run.mjs last --id <id> --chars 4000
node scripts/copilot-run.mjs log --id <id> --lines 60

# Continue the exact recorded session with a follow-up prompt.
node scripts/copilot-run.mjs resume --id <id> --prompt-file .kiln/copilot/prompts/<follow-up>.md

# Fresh availability probe (see "Availability and the fresh probe").
node scripts/copilot-run.mjs probe
```

`run` requires `--id` and `--prompt-file`. `resume` requires the same worker ID and a new prompt
file, and continues **that worker's recorded session ID** (never “the most recent session”, because
several workers may share a working directory). `probe` runs a trivial prompt through the same spawn
path and prints the observed exit code and answer, `probe [--model <name>]` taking an optional model.

The runner stays in the foreground, exactly like `scripts/codex-run.mjs`; backgrounding long jobs is
the shell's or orchestrator's job. Worker state is `.kiln/copilot/`, and for worker `<id>` the runner
writes:

```text
.kiln/copilot/<id>.meta.json
.kiln/copilot/<id>.log
.kiln/copilot/<id>.err
.kiln/copilot/<id>.last.md
.kiln/copilot/<id>.exit.json
```

## `run` options

| Option | Values | Default | Use |
|---|---|---|---|
| `--cwd <dir>` | Existing directory | Repository root | Isolate the worker's scope or worktree |
| `--model <name>` | Any slug Copilot accepts, or `auto` | **No flag passed** | Explicit model override only |
| `--effort <level>` | `none`, `minimal`, `low`, `medium`, `high`, `xhigh`, `max` | **No flag passed** | Explicit reasoning-effort override only |
| `--permissions <mode>` | `tools` (default), `tools+paths` | `tools` | `tools` → `--allow-all-tools`; `tools+paths` → `--allow-all-tools --allow-all-paths` |
| `--timeout-min <n>` | Positive integer | No runner timeout | Kill a worker past a deliberate limit |
| `--label <text>` | Human-readable text | Empty | Explain the worker in status output |
| `--agent <name>` | A Copilot agent name | None | Forward `--agent` when a slot defines one |

## Hard rules

**Never raise cost on the user's behalf.** Do **not** pass `--model` or `--reasoning-effort` unless
the caller asked for that flag. This is the rule `scripts/codex-run.mjs` was written from: its old
`--effort` default silently overrode the user's own configuration and drained the allowance. Same
failure, same fix — the runner passes neither flag by default, and the operator does not add one to
be helpful.

**Temp hygiene.** The runner always passes `--disallow-temp-dir`, and it builds the child
environment through `scratchEnv()` from `scripts/lib/scratch.mjs`, the one module that decides where
a factory process writes temporary files. Copilot must not write to the system temp directory; see
`.cursor/rules/workspace-hygiene.mdc`. Nothing in the channel names `os.tmpdir()`.

**No Copilot MCP server.** Do not add a `copilot` entry to `.cursor/mcp.json`. Nothing observed
shows Copilot exposing an MCP server mode of its own — it has `copilot mcp`, `copilot skill`,
`copilot instruction` and `copilot sessions` subcommands for managing *what Copilot consumes*, not
for serving MCP. The integration is a CLI worker channel, exactly like `scripts/codex-run.mjs`.

## Prompt contract

A worker prompt must stand alone. Include these sections in this order:

1. **Outcome:** one observable result.
2. **Scope:** exact working directory and exact files it may read or write.
3. **Inputs:** specs and facts it must read first.
4. **Hard rules:** forbidden files/actions, no secrets, and no Git mutations.
5. **Verification boundary:** exact lightweight checks the worker may run.
6. **No-Gradle rule:** “Do not run Gradle; the orchestrator owns Gradle builds” unless the worker was
   explicitly assigned a Gradle verification task.
7. **Token contract:** state the supplied token budget, require the worker to reserve enough for
   verification and handoff, and require `CONTEXT_LIMIT` before it guesses or silently drops scope.
   The runner has no separate token-budget option.
8. **Reply contract:** require a fixed, machine-scannable final form, for example “Reply with
   exactly: `DONE <files>` or `BLOCKED <reason>`.”

Example ending:

```text
Do not run Gradle. Do not edit outside the four files above. Do not run git.
Token contract: stay within the supplied budget; if the work cannot be completed
without dropping scope, reply exactly CONTEXT_LIMIT.
On success reply with exactly: DONE <comma-separated files>.
On failure reply with exactly: BLOCKED <one-line reason>.
```

A Copilot worker is a subagent-like peer, and the prompt must say so. Do not include secrets in
prompts: prompts, metadata, stdout and stderr are durable local files.

## Reading output

1. Run `status --id <id>`.
2. On `DONE`, read `last`; then read the diff and execute the required verification.
3. On `FAILED`, `BLOCKED(...)`, `LOST`, or no final message, read `log` and the `.err` tail.
4. Treat the worker's claimed tests as unverified until reproduced by the controller.
5. Use `resume` only when a narrow follow-up can fix the same task safely.

**Windows PowerShell shim.** The `copilot.ps1` shim prints a `NativeCommandError` block to **stderr
even on a successful run** (its own `node` invocation line). An exit code of 0 with a correct answer
is a success. Therefore: **judge success by the exit code plus the diagnosed signal, never by
“stderr is non-empty”.** The `.err` file accumulating a `NativeCommandError` is not, by itself, a
failure.

## Ledger verdicts

`status`/`list` derive one verdict per worker from its recorded status, its exit record, and whether
its recorded pid is still alive. The verdicts are the same four as Codex's, because they are computed
by the same module (`scripts/lib/codex-ledger.mjs`):

| Verdict | Meaning |
|---|---|
| `DONE` | Exit record with exit code 0 and no diagnosed signal |
| `FAILED(<code>)` | Exit record with a non-zero exit code and no signal |
| `BLOCKED(<signal>)` | Exit record diagnosed a signal; a signal outranks a zero exit code, because a worker that stopped on a limit has not delivered |
| `LOST` | Recorded `running`, no exit record, and the recorded pid is not alive (including no usable recorded pid) |

`DONE` is the runner's process verdict, not evidence the deliverable exists: read the diff and
reproduce the verification. A `LOST` worker died without reporting, so its work is **unverified** —
list and read its files on disk directly, and never assume the task completed. Liveness is a
heuristic with a known pid-reuse limitation: pids are recycled, so a dead worker's pid can now belong
to an unrelated live process and stay reported `running`; only `ESRCH` proves death, and `EPERM` is
treated as alive, so the check can miss a death but never invents a `LOST`.

## Classified failures

| Failure | Meaning | Reaction |
|---|---|---|
| `RATE_LIMITED` | Usage/rate/quota limit | Do not retry Copilot; preserve the task and fall through to Cursor `generator` |
| `CONTEXT_LIMIT` | Context or token limit | Reduce scope and resume once; then use `generator` |
| `AUTH_ERROR` | CLI login/session problem | Do not create or paste credentials; use `generator` and report the auth issue |

An unclassified failure gets one evidence-driven resume. If the same issue occurs twice, stop retries
and use the ladder in `harness/dual-agent.md`. Copilot's own `RATE_LIMITED` and `AUTH_ERROR` are the
end of this channel for that task: the first fallback rung is Copilot, and the next rung after it is
`generator`.

## Availability and the fresh probe

A `BLOCKED(...)` label from `list` is not evidence that Copilot is unavailable. Before assigning a
Cursor `generator`, run a fresh read-only probe in this session at the model the task will use, and
record its observed exit in the handoff:

```bash
node scripts/copilot-run.mjs probe
```

Apply the failure table to the failed worker task only: `RATE_LIMITED` does not retry and goes to
`generator`; `CONTEXT_LIMIT` reduces scope and resumes once, then goes to `generator`; `AUTH_ERROR`
records the blocker and goes to `generator`. One blocked worker does not disable Copilot while the
fresh probe answers.

Historical exit records predate the current classifier and can describe its bug rather than the
worker's fate. Check the deliverable on disk before trusting a label; see
“2026-09-23 - the failure classifier reported finished work as blocked, because it matched prose”
in `harness/lessons.md`.

## Three-worker parallel example

First create three prompts with disjoint write sets:

```text
.kiln/copilot/prompts/build-ui.md
.kiln/copilot/prompts/store-copy.md
.kiln/copilot/prompts/verify-readonly.md
```

From PowerShell, launch the foreground runners as background jobs. Note the example passes no
`--model` and no `--effort`: the channel defaults to no cost override, and an operator adds one only
when the caller explicitly asked for it.

```powershell
$kilnRoot = (Resolve-Path '.').Path
$jobs = @(
  Start-Job -Name build-ui -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/copilot-run.mjs run --id build-ui --prompt-file .kiln/copilot/prompts/build-ui.md --label 'Build UI' --timeout-min 30
  }
  Start-Job -Name store-copy -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/copilot-run.mjs run --id store-copy --prompt-file .kiln/copilot/prompts/store-copy.md --label 'Store copy' --timeout-min 30
  }
  Start-Job -Name verify-readonly -ArgumentList $kilnRoot -ScriptBlock {
    param($root) Set-Location $root
    node scripts/copilot-run.mjs run --id verify-readonly --prompt-file .kiln/copilot/prompts/verify-readonly.md --label 'Read-only verification' --timeout-min 30
  }
)
```

Poll the durable state, not the PowerShell job output:

```powershell
node scripts/copilot-run.mjs status
node scripts/copilot-run.mjs last --id build-ui
node scripts/copilot-run.mjs last --id store-copy
node scripts/copilot-run.mjs last --id verify-readonly
$jobs | Wait-Job | Receive-Job
```

The channel exposes two permission modes, `tools` and `tools+paths` (see `run` options), and neither
is a read-only mode. A Verify worker is therefore kept non-writing by its prompt contract and by
giving it no file to write in its brief; the authoritative read-only pass stays with the orchestrator.
If two workers would edit the same file, serialize them or place them in separate worktrees.
