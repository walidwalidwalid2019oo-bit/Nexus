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