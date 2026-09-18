# Rules & Project Guidelines: Ponytail & YAGNI Mode

## Status: ALWAYS ACTIVE AUTOMATICALLY
The **Ponytail** methodology is permanently active across all tasks, responses, and code changes in this project without needing to trigger `/ponytail`.

## Core Principle: YAGNI (You Aren't Gonna Need It)
- **Do not write speculative code**: If a feature, helper, abstraction, config, or parameter is not needed right now for the immediate user request, DO NOT write it.
- **Question every addition**: The best code is the code that is never written. Deletion over addition. Boring over clever.
- **No unrequested scaffolding**: No interfaces with a single implementation, no factories for one product, no speculative abstractions "for later". Later can build for itself.

## The Ladder of Solution (Stop at the first rung that holds)
1. **Does this need to exist at all? (YAGNI)**: Speculative need = skip it.
2. **Already in this codebase?**: Look before writing; reuse existing helpers, models, and utilities.
3. **Android / Kotlin stdlib does it?**: Use native platform capabilities and standard libraries first.
4. **Already-installed dependency solves it?**: Use existing libraries (Jsoup, OkHttp, Coil, Room). Never add a new dependency when a few lines of native code suffice.
5. **Shortest working diff wins**: Keep changes minimal, surgical, and localized to the root cause.

## Bug Fixing Standard
- Fix the root cause, not the symptom.
- One guard in the shared handler/function is better than patching multiple callers.

## Build & Compilation Policy: NO AUTOMATIC APK BUILDS
- **NEVER compile or build APKs automatically**: Do NOT execute `./gradlew assembleRelease`, `assembleDebug`, or `adb install` after creating a feature or fixing a bug.
- **User Verification First**: Always leave the code ready for the user to test and verify first, saving token costs and avoiding unnecessary build time.
- **Build ONLY on Explicit Command**: Only build an APK when the user explicitly requests it (e.g., "build apk", "buat apk", "compile apk").

## Response Style
- Code first.
- Direct, concise, and focused. Avoid unnecessary fluff and unrequested essays.

