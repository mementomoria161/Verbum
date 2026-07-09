# Verbum

A minimalist, typography-first Android launcher. No icons — apps are text.
Everything lives on an invisible, resizable 4-column grid of square cells,
similar to the modern Android Quick Settings panel.

## Features

- **Home screen replacement** — set Verbum as your default launcher.
- **Typography only** — apps are shown as centered text, never icons.
- **Modular grid** — blocks (the all-apps list and named folders) can be
  moved and resized on a 4-column grid, Quick-Settings style.
- **Search** — the large side of the bottom split button opens live
  as-you-type filtering; pressing Go launches a single remaining match.
- **Customize** — the small side of the split button opens a sheet with:
  background color or local image, global text size and color, custom
  `.ttf`/`.otf` font import, hidden-app management, and the homescreen
  edit mode.
- **Edit ("jiggle") mode** — drag blocks to move, drag the corner dot to
  resize, tap to rename, add new folder blocks. Also reachable by
  long-pressing an empty spot on the homescreen.
- **App management** — long-press any app name to open, rename, hide, or
  move it into a folder.

## Tech stack

- Kotlin (K2), Jetpack Compose, Material 3
- MVVM with `StateFlow`
- Preferences DataStore + kotlinx.serialization for persistence
- No XML layouts

## Building

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator (minSdk 26).
3. Press the home button and choose **Verbum → Always** to set it as the
   default launcher (or Settings → Apps → Default apps → Home app).

### Version notes

`gradle/libs.versions.toml` targets the versions from the project spec
(AGP 9.2.1, Kotlin 2.4.0, Gradle 9.6.1). Because AGP 9.0+ ships **built-in
Kotlin support**, this project does *not* apply the standalone
`org.jetbrains.kotlin.android` plugin — only the Compose and serialization
compiler plugins. If Gradle sync fails to resolve the spec versions, switch
to the fallback set noted at the top of that file (AGP 8.13.0, Kotlin
2.2.20, Gradle wrapper 8.13); on AGP below 9.0 you must also re-add the
Kotlin Android plugin, as described in that file's comment.

The Gradle wrapper JAR is intentionally not committed. If Android Studio
asks, let it generate/use the wrapper, or run `gradle wrapper` once from a
local Gradle installation.
