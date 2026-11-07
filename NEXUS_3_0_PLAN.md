# NEXUS 3.0 — Upgrade plan

NEXUS 3.0 is a staged expansion of the existing Android app. It preserves the current TITAN, VOID, NEXUS AI, CREATIVE AI, DARK SIDE, LOST MIND, Memory, Vault and Project Map experiences.

## Ten upgrade tracks

1. **Command Center 2.0** — unified overview and shortcuts. The first foundation view reports existing local mode, project and note counts.
2. **AI Council** — structured multi-AI review workflow. The first foundation creates a reusable briefing; actual parallel model orchestration is a later integration.
3. **Project Studio** — local project list with add/remove and persistent storage. Milestones, task boards and visual dependencies are next.
4. **Voice System** — Android text-to-speech. Speech recognition and live duplex voice are later work.
5. **File Lab** — Android document picker with local text previews and copy-to-clipboard for TXT, MD, JSON, CSV, XML, Kotlin, Python and LOG files. PDF extraction and AI analysis are later work.
6. **Memory 2.0** — status overview of existing per-mode memory and LOST MIND notes. Timeline search, tagging and import/export are next.
7. **Automation Engine** — local routine preference toggle. Actual scheduled background jobs and notifications are later work.
8. **NEXUS Extensions** — local extension preference switches. A safe plugin API, permissions and install/update lifecycle are later work.
9. **Creative Studio 2.0** — reusable creative brief builder. Provider-backed image/audio/video generation is a separate integration.
10. **Security & Backup** — reports the existing Android Keystore encrypted API-key state without exposing secrets. App lock and export/restore are later work.

## Foundation build scope

The NEXUS 3.0 screen is reachable from Home and contains all ten modules. Implemented interactions in this foundation are local project persistence, TTS, document selection, local text-file previews and copying, council/creative brief copying, routine preference storage, extension preference storage and local status summaries.

## Important boundaries

- No API secret is embedded in source code, APK resources, or the repository.
- A client app cannot safely hide a shared provider key. Shared API access needs a secured backend with server-side secrets, abuse controls and usage limits.
- A visible module is not the same as a completed cloud integration. The module descriptions identify what is working now and what remains.
- Do not claim online music streaming, AI Council parallel execution, document understanding, background automation, plugin downloads, image generation, app lock or backup/restore until those integrations are implemented and tested.
