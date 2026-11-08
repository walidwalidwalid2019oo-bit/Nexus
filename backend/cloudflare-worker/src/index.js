/**
 * NEXUS private AI gateway
 * Deploy as a Cloudflare Worker. Provider credentials belong in Worker secrets,
 * never in the Android app or this repository.
 */
const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store",
  "x-content-type-options": "nosniff",
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), { status, headers: JSON_HEADERS });
}

function allowedModels(raw) {
  return new Set((raw || "").split(",").map(x => x.trim()).filter(Boolean));
}

async function rateLimit(request, env) {
  // A KV namespace named RATE_LIMIT_KV must be bound at deployment.
  if (!env.RATE_LIMIT_KV) return false;
  const ip = request.headers.get("CF-Connecting-IP") || "unknown";
  const minute = Math.floor(Date.now() / 60000);
  const key = "rl:" + minute + ":" + ip;
  const current = Number(await env.RATE_LIMIT_KV.get(key) || "0");
  if (current >= Number(env.REQUESTS_PER_MINUTE || 12)) return true;
  await env.RATE_LIMIT_KV.put(key, String(current + 1), { expirationTtl: 120 });
  return false;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "GET" && url.pathname === "/health") {
      return json({ service: "NEXUS AI Gateway", status: "ok" });
    }
    if (request.method !== "POST" || url.pathname !== "/v1/chat/completions") {
      return json({ error: { message: "Not found" } }, 404);
    }
    if (await rateLimit(request, env)) {
      return json({ error: { message: "Rate limit reached. Try again shortly." } }, 429);
    }

    const declaredLength = Number(request.headers.get("content-length") || "0");
    if (declaredLength > 12 * 1024 * 1024) {
      return json({ error: { message: "Request is too large (12 MB maximum)." } }, 413);
    }

    let body;
    try {
      const raw = await request.text();
      if (new TextEncoder().encode(raw).length > 12 * 1024 * 1024) {
        return json({ error: { message: "Request is too large (12 MB maximum)." } }, 413);
      }
      body = JSON.parse(raw);
    } catch {
      return json({ error: { message: "Expected valid JSON." } }, 400);
    }

    const provider = String(body.provider || "pollinations").toLowerCase();
    const messages = body.messages;
    if (!["pollinations", "groq"].includes(provider)) {
      return json({ error: { message: "Provider must be pollinations or groq." } }, 400);
    }
    if (!Array.isArray(messages) || messages.length < 1 || messages.length > 30 ||
        messages.some(m => !m || !["system", "user", "assistant"].includes(m.role) ||
          !(typeof m.content === "string" || Array.isArray(m.content)))) {
      return json({ error: { message: "Invalid messages. Send 1–30 chat messages." } }, 400);
    }

    const isPollinations = provider === "pollinations";
    const secret = isPollinations ? env.POLLINATIONS_API_KEY : env.GROQ_API_KEY;
    if (!secret) return json({ error: { message: provider + " is not configured on the server." } }, 503);

    const model = String(body.model || "");
    const modelSet = allowedModels(isPollinations ? env.POLLINATIONS_MODELS : env.GROQ_MODELS);
    if (!model || !modelSet.has(model)) {
      return json({ error: { message: "That model is not enabled for this provider." } }, 400);
    }

    const safeBody = {
      model,
      messages,
      stream: false,
      max_tokens: Math.min(Math.max(Number(body.max_tokens || 900), 1), 1800),
    };
    const upstream = isPollinations
      ? "https://gen.pollinations.ai/v1/chat/completions"
      : "https://api.groq.com/openai/v1/chat/completions";

    try {
      const response = await fetch(upstream, {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + secret,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(safeBody),
        signal: AbortSignal.timeout(55000),
      });
      const responseText = await response.text();
      let parsed;
      try { parsed = JSON.parse(responseText); }
      catch { parsed = { error: { message: "The AI provider returned an unreadable response." } }; }
      // Do not expose upstream headers or credentials.
      return json(parsed, response.status);
    } catch {
      return json({ error: { message: "The AI provider could not be reached. Try again later." } }, 502);
    }
  },
};
