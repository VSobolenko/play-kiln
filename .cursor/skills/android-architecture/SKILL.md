---
name: android-architecture
description: Default Android architecture for PlayKiln slots — single module first, UDF, layering when growing. Use when designing or reviewing slot structure.
---

# Android architecture (factory default)

**Start:** single `:app` module (speed of kiln attempts).

**Grow when OpenSpec says so:**

```
:app          → navigation host, DI graph
:core:ui      → theme, shared composables
:core:data    → repository, DataStore/Room
:feature:*    → vertical slices
```

Rules:

- UI → domain/data one way; no feature↔feature cycles
- UDF: intents/events in, `UiState` out
- Prefer coroutines/Flow; KSP over kapt
- Tests next to behavior that can break Play review
