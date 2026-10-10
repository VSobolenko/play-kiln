package com.gexetr.lumintralightplanner.ui.theme

import androidx.compose.ui.graphics.Color

// Lumintra's palette, and the one source of truth for store/studio.json's `palette`: every studio
// role names one of these constants by value. A night field that is nearly neutral (C <= 12), one
// warm "first light" ember reserved for the light the user is planning around (golden and blue
// hour, the Sun/Moon on its arc), one cool moonlight that marks the Moon's interference, and one
// aurora green that means "dark enough". Red night-vision mode lives in NightVisionPalette.kt.

// The night field — studio `ink` / `ink_soft` / `ink_lift`.
internal val NightInk = Color(0xFF0B0E14)
internal val NightInkSoft = Color(0xFF141924)
internal val NightInkLift = Color(0xFF262A33)
internal val NightContainerLow = Color(0xFF10141C)
internal val NightContainer = Color(0xFF191D26)
internal val NightContainerHigh = Color(0xFF20242D)
internal val NightContainerHighest = Color(0xFF2C303A)
internal val NightOutline = Color(0xFF8A91A3)
internal val NightOutlineVariant = Color(0xFF3A4254)

// Starlight — studio `paper` (display text, the mark's arc) and `muted` (secondary text).
internal val Starlight = Color(0xFFF3F1EC)
internal val StarlightMuted = Color(0xFFA9B0C2)

// Ember, the first light — studio `amber` / `amber_deep` / `amber_pale`.
internal val Ember = Color(0xFFFF8F6B)
internal val EmberDeep = Color(0xFF886156)
internal val EmberPale = Color(0xFFFFD2C2)
internal val EmberInk = Color(0xFF3A1206)
internal val EmberNight = Color(0xFF5A2312)

// Moonlight — studio `secondary` and `accent_deep` (the cool glow in the field).
internal val Moonlight = Color(0xFFB4C0FF)
internal val MoonDeep = Color(0xFF333E7A)
internal val MoonDay = Color(0xFF4553A6)
internal val MoonPale = Color(0xFFDDE2FF)
internal val MoonInk = Color(0xFF18204A)

// Aurora — studio `tertiary`: the dark window is open.
internal val Aurora = Color(0xFF7FD8C4)
internal val AuroraDeep = Color(0xFF00695A)
internal val AuroraNight = Color(0xFF11503F)
internal val AuroraPale = Color(0xFFB5F0E2)
internal val AuroraInk = Color(0xFF00382F)

// Error, both schemes.
internal val AlertDay = Color(0xFFB3261E)
internal val AlertDayContainer = Color(0xFFF9DEDC)
internal val AlertDayInk = Color(0xFF410E0B)
internal val AlertNight = Color(0xFFFFB4AB)
internal val AlertNightInk = Color(0xFF690005)
internal val AlertNightContainer = Color(0xFF93000A)
internal val AlertNightPale = Color(0xFFFFDAD6)

// The day chrome (light scheme), still the same identity: a warm paper field and night-ink text.
internal val DayPaper = Color(0xFFF5F4F1)
internal val DaySurface = Color(0xFFFCFBF9)
internal val DayContainerLowest = Color(0xFFFFFFFF)
internal val DayContainerLow = Color(0xFFF0EFEB)
internal val DayContainer = Color(0xFFEBE9E5)
internal val DayContainerHigh = Color(0xFFE5E3DF)
internal val DayContainerHighest = Color(0xFFDFDDD9)
internal val DayVariant = Color(0xFFE8E6E1)
internal val DayInk = Color(0xFF151820)
internal val DayInkMuted = Color(0xFF474C59)
internal val DayOutline = Color(0xFF767B88)
internal val DayOutlineVariant = Color(0xFFC7C9D0)
internal val DayInverse = Color(0xFF2A2E38)
internal val DayInverseInk = Color(0xFFF1F0EC)

// The sky field the hero panels and the Night scene's chrome are painted on, in every theme:
// a night sky on a light-mode screen is still a night sky.
internal val SkyZenith = Color(0xFF070A10)
internal val SkyHorizon = Color(0xFF1C2333)
internal val BlueHour = Color(0xFF6F86D9)
internal val CloudVeil = Color(0xFF8E95A6)

// The device mock-up frame the store compositions draw — studio `bezel` / `bezel_rim`.
internal val Bezel = Color(0xFF1C1A17)
internal val BezelRim = Color(0xFF8C8A86)

internal val Scrim = Color(0xFF000000)
