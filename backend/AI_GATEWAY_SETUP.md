# NEXUS private AI gateway (Pollinations + Groq)

This is a backend and Android-client foundation, not a deployed service. Provider credentials must stay on the server and must never be shipped in the Android app.

## What is included
- Cloudflare Worker routes requests to Pollinations or Groq using a server-side provider choice.
- Provider API keys are read from Worker secrets.
- Only explicitly configured model IDs are accepted.
- Request size, message count, and maximum output tokens are limited.
- A KV-backed per-IP request counter is required; the Worker fails closed with HTTP 503 if the KV binding is missing.
- The Android chat and CREATIVE AI code can call the gateway when its HTTPS endpoint is configured.
- The Android gateway client sends no provider API key.
- The gateway returns no-store JSON and does not intentionally persist chat messages.

## Before deployment
1. A parent/guardian should own and supervise the hosting/provider accounts and any billing.
2. Create a Cloudflare Worker and a KV namespace, then bind that namespace as `RATE_LIMIT_KV`.
3. Replace the placeholder KV namespace ID in `wrangler.toml`.
4. Add secrets in the hosting dashboard (never put values in GitHub):
   - `POLLINATIONS_API_KEY`
   - `GROQ_API_KEY`
5. Configure the allowed model IDs in `POLLINATIONS_MODELS` and `GROQ_MODELS`.
6. Deploy and test `GET /health`.
7. Before sharing the endpoint, add stronger per-install/user authentication, durable rate limiting, spending alerts, and provider budget limits.

## Android configuration
After a trusted Worker is deployed, set the Android app's API endpoint to:

`https://YOUR-WORKER-DOMAIN/v1/chat/completions`

The app can then leave its provider API key field blank for gateway requests. The current code selects Groq for model IDs containing `llama` or `groq`; other model IDs are routed to Pollinations. The selected model must also be present in the matching server-side allowlist.

Until a real endpoint is configured, the app continues using its existing direct-provider configuration. The endpoint is intentionally not hardcoded into the APK or repository.

## Build and testing
The GitHub Actions workflow builds a debug APK for pushes to the gateway branch and pull requests targeting `main`. A successful build proves compilation only; it does not prove that a deployed gateway or provider credentials work.

## Important release blockers
- This Worker is not deployed by committing these files. It requires a hosting account, KV binding, provider secrets, and deployment.
- The KV counter is a basic abuse-control layer, not strong per-user authentication or a billing guarantee. Do not publish a shared endpoint until stronger authentication, durable rate limiting, spending alerts, and provider budget limits are in place.
- The Android app has not been tested against a live gateway. Do not remove the existing direct-provider configuration until the deployed gateway is tested.
- Remove API-key entry/storage from the public release app once the gateway is proven, and rotate any key that was ever committed publicly.
- Do not add real API keys, bearer tokens, or deployed endpoint secrets to this repository.

## Privacy
The gateway forwards submitted messages to the selected AI provider. It does not intentionally store chat content. Provider retention and data handling still apply; disclose this accurately in the NEXUS privacy policy.
