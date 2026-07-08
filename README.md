# Robocop

An Android text-expansion app: type a short trigger anywhere on the device and have it
auto-expand into full text, an AI prompt, an image, a script's output, or an interactive
form — without switching keyboards.

## Features

- **Text expansion** — define a trigger (e.g. `:sig`) that expands to a text block, email
  signature, or image, in any app, via an Accessibility Service (no custom IME required).
  Supports `${cursor}`, `${date}`, `${time}`, and `${clipboard}` placeholders.
- **Workflow automation** — snippets can run a shell script and insert its output, or store
  AI prompts for quick reuse.
- **Auto-correct** — ~40 built-in common-typo corrections, plus custom entries for brand
  names/technical terms, applied automatically at word boundaries as you type.
- **Form filling** — snippets can define a small form (text fields, dropdowns) shown as an
  overlay; submitting renders a template with the field values and inserts it into the
  original field.
- **Quick search** — a draggable floating bubble that opens a snippet search box over
  whatever app is in the foreground.

## Building

This was written and reviewed in an environment without an Android SDK or network access
to Google's Maven repo, so it has **not been compiled or run**. To build it:

1. Open the repo root in Android Studio (Iguana or newer recommended).
2. Let Gradle sync — it needs network access to `google()` and `mavenCentral()` to resolve
   AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00, Room 2.6.1, Navigation Compose 2.7.7.
3. Run on a device/emulator with API 26+ (minSdk 26, targetSdk/compileSdk 34).

Since none of this has been compiled, a normal review for typos/signature mismatches won't
catch everything a real Gradle build would — do a build before relying on it.

## Required permissions (granted at runtime, not install time)

- **Accessibility service** — Settings → enable "Robocop" (also linked from the app's
  Settings tab). Required for text expansion and auto-correct to see what you type and
  edit the focused field.
- **Display over other apps** — required only for the quick-search bubble.
- **Notifications** — the quick-search bubble runs as a foreground service and needs a
  (minimal-priority) notification while active.

## Known limitations

- **AppleScript is not supported.** It's a macOS-only scripting language with no Android
  equivalent; shell scripts are supported instead.
- **Python execution is best-effort, not native.** Android has no built-in Python runtime.
  Python snippets are sent to [Termux](https://termux.dev/) + Termux:API's `RUN_COMMAND`
  intent if installed; this requires `allow-external-apps=true` in
  `~/.termux/termux.properties`, and the script's output is not captured back into the
  app (Termux runs it in its own session).
- **Shell scripts run with the app's own process permissions** (no root) and time out
  after 15 seconds.
- Text injection uses `ACTION_SET_TEXT` where supported, falling back to a clipboard-paste
  trick otherwise; a small number of custom input widgets may not support either.
