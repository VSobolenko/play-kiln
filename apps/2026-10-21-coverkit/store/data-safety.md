# Data safety answers (draft) — Coverkit: Social Studio

Complete before Play Console submission. Must match the privacy policy and actual
code/SDK behavior. Even apps that collect **no** user data must file the form.

## Does the app collect user data?

- [ ] No — explain briefly why (e.g. fully offline, no analytics, no accounts)
- [ ] Yes — list categories below

## Data types (check all that apply)

| Category | Collected? | Shared? | Purpose | Ephemeral? |
|----------|------------|---------|---------|------------|
| App activity | | | | |
| Location | | | | |
| Personal info | | | | |
| Financial info | | | | |
| Photos / videos | | | | |
| Files / docs | | | | |
| Device IDs | | | | |
| Diagnostics / crash | | | | |

## Third-party SDKs

List every SDK that may collect data (Ads, Analytics, Crash, Auth, AI APIs):

1. …
2. …

## Security practices

- [ ] Data encrypted in transit (HTTPS)
- [ ] Users can request deletion (if accounts exist)
- [ ] Account deletion path documented (Play requirement when accounts exist)

## Agent note

Run `.agents/skills/android-official/play-play-policy-insights/` before marking ready.
