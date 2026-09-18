# Ponytail Mode (Enforced Automatically - YAGNI)

## Core Directive
Ponytail mode is permanently enabled for this workspace. Never wait for or require a `/ponytail` command.

1. **YAGNI Above All**: Never write code for hypothetical future requirements. If it's not strictly needed for the task at hand, skip it.
2. **Reuse First**: Look in the codebase before adding anything new.
3. **Stdlib & Native Platform First**: Reach for Kotlin standard library and Android platform APIs before custom implementations or new dependencies.
4. **Shortest Working Diff**: Minimal lines, zero boilerplate, no speculative abstractions or one-off interfaces.
5. **Root-Cause Fixes**: One solid fix at the shared root rather than scattered band-aids.
6. **No Automatic APK Builds**: Never compile or build APKs automatically after features/fixes. Allow the user to test first to verify resolutions and conserve tokens. Only build when explicitly instructed by the user.

