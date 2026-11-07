# NEXUS

Native Android workspace built with Kotlin and Jetpack Compose.

## Features
- Four independent AI spaces: TITAN, VOID, NEXUS AI, and CREATIVE AI
- Separate conversation history for each AI mode
- Configurable OpenAI-compatible HTTPS chat API
- API key encrypted with Android Keystore
- LOST MIND notes stored locally
- Independent per-mode memory spaces
- Deep-black and dark-purple interface

## Configure live AI
Open Settings and enter an HTTPS chat-completions endpoint, the provider's model ID, and an API key. The defaults are prefilled for Groq. A valid provider key is required; provider usage, account eligibility and possible charges depend on the provider. Do not share your key.

## Build
Pushes to main run the GitHub Actions workflow **Build NEXUS APK**. Download the `NEXUS-debug-APK` artifact from the successful run.

## Not implemented yet
AI image/audio/video generation and actual cross-mode memory sharing are not included in this early test build.

## NEXUS 3.0 foundation
The Home screen now includes a NEXUS 3.0 upgrade hub with ten modules: Command Center, AI Council briefing builder, Project Studio, Voice System, File Lab, Memory overview, Automation preferences, Extensions preferences, Creative Studio brief builder, and Security status.

The first foundation includes local project persistence, Android text-to-speech, document selection, copyable AI/creative briefs, and locally saved routine/extension preferences. Some modules are foundations rather than finished integrations; see [NEXUS_3_0_PLAN.md](NEXUS_3_0_PLAN.md) for the exact implemented scope and remaining work.
