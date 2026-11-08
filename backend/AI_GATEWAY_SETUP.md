# NEXUS private AI gateway (Pollinations + Groq)

This is a backend foundation, not a deployed service. The Android app must not contain either provider secret.

## What it does
- Routes requests to Pollinations or Groq using a server-side provider choice.
- Keeps provider API keys in Cloudflare Worker secrets.
- Allows only explicitly configured model IDs.
- Limits request size, message count, and max output tokens.
- Applies a basic per-IP request counter using Cloudflare KV.
- Returns no-store JSON and does not persist chat messages.

## Before deployment
1. A parent/guardian should own and supervise the hosting/provider accounts and billing.
2. Create a Cloudflare Worker and a KV namespace, then bind that namespace as `RATE_LIMIT_KV`.
3. Replace the placeholder KV namespace ID in `wrangler.toml`.
4. Add secrets in the hosting dashboard (do not put values in GitHub):
   - `POLLINATIONS_API_KEY`
   - `GROQ_API_KEY`
5. Configure the allowed model IDs in `POLLINATIONS_MODELS` and `GROQ_MODELS`.
6. Deploy and test `GET /health`.

## Request format
POST `/v1/chat/completions`:
```json
{
  "provider": "pollinations",
  "model": "openai/gpt-oss-120b",
  "messages": [{"role":"user","content":"Hello"}],
  "max_tokens": 900
}
```
Use `"provider":"groq"` and a Groq model from the configured allowlist to route to Groq.

## Important release blockers
- This Worker is not deployed by committing these files. It needs a hosting account, KV binding, secrets, and a deployment.
- The KV counter is a basic abuse-control layer, not strong per-user authentication or a billing guarantee. Before public release, add a stronger per-install/user authentication flow, durable rate limiting, spending alerts, and provider budget limits.
- The Android app has not yet been switched to this gateway. Until a gateway URL is deployed and tested, the app continues using its existing direct-provider configuration.
- Remove any old API key from the Android app's settings/export path in the release build. If a key was ever committed publicly, rotate it with its provider.
- Do not add real API keys, bearer tokens, or deployed endpoint secrets to this repository.

## Privacy
The gateway forwards the submitted messages to the selected AI provider. It does not intentionally store chat content. Provider retention and data handling still apply; disclose this accurately in the NEXUS privacy policy.
