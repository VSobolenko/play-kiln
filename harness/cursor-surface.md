# Cursor surface map (PlayKiln)

Official Cursor agent configuration surfaces (2026). Skills / hooks / commands / MCP
are only part of it. This factory uses **all** project-level surfaces that matter.

| Surface | Path | Role | PlayKiln |
|---------|------|------|----------|
| **AGENTS.md** | `/AGENTS.md` + nested | Cross-tool always-on contract (Cursor **and** Codex) | Yes + slot nested |
| **DESIGN.md** | `/DESIGN.md` | Visual/UX always-on (Factory.ai-style) | Yes |
| **Rules** | `.cursor/rules/*.mdc` | always / globs / intelligent / @manual | Yes |
| **Skills** | `.cursor/skills/`, `.agents/skills/` | On-demand workflows | Yes |
| **Commands** | `.cursor/commands/*.md` | User slash entry points | Yes (`/forge`, `/ship`, `/release`, `/eval`…) |
| **Custom subagents** | `.cursor/agents/*.md` | Isolated specialists (Planner/Generator/Evaluator…) | **Yes — required** |
| **Hooks** | `.cursor/hooks.json` | Deterministic lifecycle gates | Expanded |
| **MCP** | `.cursor/mcp.json` | External tools | Yes |
| **Ignore** | `.cursorignore` | Keep secrets/build junk out of Agent index | Yes |
| Team Rules / User Rules | Dashboard / Customize | Org/global — not in repo | Documented only |
| Cloud Agents MCP | cursor.com team config | Separate from local mcp.json | Documented |
| Plugins / Marketplace | Customize | Optional installs | Prefer committed mcp.json |

## Hooks we use

| Event | Why |
|-------|-----|
| `sessionStart` | Inject kiln L1 context every chat |
| `preToolUse` | secret-guard on writes |
| `beforeShellExecution` | git-guard |
| `beforeMCPExecution` | soft audit (never invent tools) |
| `afterFileEdit` | lightweight edit ledger |
| `stop` | one evidence follow-up (factory back-pressure) |

## Subagents vs skills vs commands

- **Command** — human starts a factory phase (`/forge` finds the topic and builds it end to end, `/ship` develops an idea already in hand, `/release` ships after approval)
- **Skill** — agent loads procedure when relevant
- **Subagent** — separate context window for Planner / Generator / Evaluator / auditor

Built-in Cursor subagents (`explore`, `bash`, `browser`) still run automatically.
Project agents add **factory roles**.

## Sources

- https://cursor.com/docs/context/rules
- https://cursor.com/docs/subagents
- https://cursor.com/docs/hooks
- https://cursor.com/docs/mcp
- https://cursor.com/docs/reference/ignore-file
- https://cursor.com/docs/skills
