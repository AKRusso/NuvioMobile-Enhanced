interface Env {
  DB: D1Database;
  KOFI_VERIFICATION_TOKEN: string;
  ADMIN_TOKEN: string;
}

interface KofiPayload {
  verification_token?: unknown;
  message_id?: unknown;
  timestamp?: unknown;
  type?: unknown;
  is_public?: unknown;
  from_name?: unknown;
  message?: unknown;
}

interface DonationRow {
  id: string;
  name: string;
  donated_at: string;
  message: string | null;
  avatar: string | null;
  profile: string | null;
}

interface GitHubContributor {
  login?: string;
  avatar_url?: string;
  html_url?: string;
  contributions?: number;
  type?: string;
}

const MAX_BODY_BYTES = 64 * 1024;
const MAX_RECENT_DONATIONS = 50;
const CONTRIBUTIONS_URL =
  "https://api.github.com/repos/AKRusso/NuvioMobile-Enhanced/contributors?per_page=100";
const APPROVED_AVATAR_HOSTS = new Set([
  "avatars.githubusercontent.com",
  "cdn.discordapp.com",
  "images.ko-fi.com",
  "media.discordapp.net",
  "storage.ko-fi.com",
]);

const publicHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Access-Control-Max-Age": "86400",
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);

    try {
      if (request.method === "GET" && url.pathname === "/healthz") {
        await env.DB.prepare("SELECT 1").first();
        return jsonResponse({ ok: true }, 200, { "Cache-Control": "no-store" });
      }

      if (url.pathname === "/api/donations") {
        if (request.method === "OPTIONS") {
          return new Response(null, { status: 204, headers: publicHeaders });
        }
        if (request.method === "GET") {
          return getDonations(env);
        }
        return methodNotAllowed("GET, OPTIONS");
      }

      if (url.pathname === "/api/contributors") {
        if (request.method === "OPTIONS") {
          return new Response(null, { status: 204, headers: publicHeaders });
        }
        if (request.method === "GET") {
          return getContributors();
        }
        return methodNotAllowed("GET, OPTIONS");
      }

      if (url.pathname === "/webhooks/kofi") {
        if (request.method !== "POST") return methodNotAllowed("POST");
        return receiveKofiWebhook(request, env);
      }

      if (url.pathname.startsWith("/api/admin/donations/")) {
        const id = decodeURIComponent(url.pathname.substring("/api/admin/donations/".length));
        if (request.method === "PATCH") return updateDonationProfile(request, env, id);
        if (request.method === "DELETE") return deleteDonation(request, env, id);
        return methodNotAllowed("PATCH, DELETE");
      }

      return jsonResponse({ error: "Not found" }, 404);
    } catch {
      return jsonResponse({ error: "Service unavailable" }, 503, {
        "Cache-Control": "no-store",
      });
    }
  },
} satisfies ExportedHandler<Env>;

async function receiveKofiWebhook(request: Request, env: Env): Promise<Response> {
  const body = await readBodyWithinLimit(request);
  if (body === null) return jsonResponse({ error: "Payload too large" }, 413);
  if (!request.headers.get("Content-Type")?.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
    return jsonResponse({ error: "Invalid form payload" }, 400);
  }

  const encodedPayload = new URLSearchParams(new TextDecoder().decode(body)).get("data");
  if (!encodedPayload || encodedPayload.length > MAX_BODY_BYTES) {
    return jsonResponse({ error: "Invalid webhook payload" }, 400);
  }

  let payload: KofiPayload;
  try {
    payload = JSON.parse(encodedPayload) as KofiPayload;
  } catch {
    return jsonResponse({ error: "Invalid webhook payload" }, 400);
  }

  const providedToken = stringValue(payload.verification_token);
  if (!providedToken || !(await secretsEqual(providedToken, env.KOFI_VERIFICATION_TOKEN))) {
    return jsonResponse({ error: "Unauthorized" }, 401);
  }

  const isPublic = payload.is_public === true || payload.is_public === "true";
  if (payload.type !== "Donation" || !isPublic) {
    return jsonResponse({ ok: true }, 200, { "Cache-Control": "no-store" });
  }

  const id = boundedRequiredString(payload.message_id, 128);
  const name = boundedRequiredString(payload.from_name, 200);
  const timestamp = normalizeTimestamp(payload.timestamp);
  const message = boundedOptionalString(payload.message, 1000);
  if (!id || !name || !timestamp || message === undefined) {
    return jsonResponse({ error: "Invalid donation payload" }, 400);
  }

  await env.DB.prepare(
    `INSERT INTO donations (id, name, donated_at, message)
     VALUES (?, ?, ?, ?)
     ON CONFLICT(id) DO NOTHING`,
  )
    .bind(id, name, timestamp, message)
    .run();

  return jsonResponse({ ok: true }, 200, { "Cache-Control": "no-store" });
}

async function getDonations(env: Env): Promise<Response> {
  const [recent, count] = await Promise.all([
    env.DB.prepare(
      `SELECT id, name, donated_at, message, avatar, profile
       FROM donations
       ORDER BY donated_at DESC, id DESC
       LIMIT ?`,
    )
      .bind(MAX_RECENT_DONATIONS)
      .all<DonationRow>(),
    env.DB.prepare(
      "SELECT COUNT(DISTINCT lower(name)) AS supporter_count FROM donations",
    ).first<{ supporter_count: number }>(),
  ]);

  const donations = recent.results.map((row) => ({
    id: row.id,
    name: row.name,
    date: row.donated_at.substring(0, 10),
    createdAt: row.donated_at,
    message: row.message,
    avatar: row.avatar,
    profile: row.profile,
  }));

  return jsonResponse(
    {
      currency: null,
      monthlyGoal: null,
      supporterCount: count?.supporter_count ?? donations.length,
      donations,
    },
    200,
    {
      ...publicHeaders,
      "Cache-Control": "public, max-age=60",
    },
  );
}

async function getContributors(): Promise<Response> {
  const response = await fetch(CONTRIBUTIONS_URL, {
    headers: {
      Accept: "application/vnd.github+json",
      "User-Agent": "NuvioMobile-Enhanced",
    },
  });
  if (!response.ok) {
    return jsonResponse({ error: "Contributors unavailable" }, 502, publicHeaders);
  }

  const contributors = (await response.json()) as GitHubContributor[];
  return jsonResponse(
    {
      contributors: contributors
        .filter(
          (contributor) =>
            contributor.type === "User" &&
            typeof contributor.login === "string" &&
            typeof contributor.contributions === "number" &&
            contributor.contributions > 0,
        )
        .map((contributor) => ({
          name: contributor.login,
          avatar: contributor.avatar_url ?? null,
          profile: contributor.html_url ?? null,
          total: contributor.contributions,
        })),
    },
    200,
    {
      ...publicHeaders,
      "Cache-Control": "public, max-age=3600",
    },
  );
}

async function updateDonationProfile(
  request: Request,
  env: Env,
  id: string,
): Promise<Response> {
  if (!id || id.length > 128) {
    return jsonResponse({ error: "Invalid request" }, 400, { "Cache-Control": "no-store" });
  }

  if (!(await isAdminAuthorized(request, env))) {
    return jsonResponse({ error: "Unauthorized" }, 401, { "Cache-Control": "no-store" });
  }

  const encodedBody = await readBodyWithinLimit(request);
  if (encodedBody === null) {
    return jsonResponse({ error: "Payload too large" }, 413, { "Cache-Control": "no-store" });
  }

  let body: Record<string, unknown>;
  try {
    body = JSON.parse(new TextDecoder().decode(encodedBody)) as Record<string, unknown>;
  } catch {
    return jsonResponse({ error: "Invalid JSON" }, 400, { "Cache-Control": "no-store" });
  }

  const hasAvatar = Object.prototype.hasOwnProperty.call(body, "avatar");
  const hasProfile = Object.prototype.hasOwnProperty.call(body, "profile");
  if (!hasAvatar && !hasProfile) {
    return jsonResponse({ error: "Nothing to update" }, 400, { "Cache-Control": "no-store" });
  }

  const avatar = hasAvatar ? safeAvatarUrl(body.avatar) : null;
  const profile = hasProfile ? safeHttpsUrl(body.profile) : null;
  if ((hasAvatar && avatar === undefined) || (hasProfile && profile === undefined)) {
    return jsonResponse({ error: "Invalid URL" }, 400, { "Cache-Control": "no-store" });
  }

  const result = await env.DB.prepare(
    `UPDATE donations
     SET avatar = CASE WHEN ? = 1 THEN ? ELSE avatar END,
         profile = CASE WHEN ? = 1 THEN ? ELSE profile END
     WHERE id = ?`,
  )
    .bind(hasAvatar ? 1 : 0, avatar, hasProfile ? 1 : 0, profile, id)
    .run();

  if (result.meta.changes === 0) {
    return jsonResponse({ error: "Not found" }, 404, { "Cache-Control": "no-store" });
  }
  return new Response(null, { status: 204, headers: { "Cache-Control": "no-store" } });
}

async function deleteDonation(request: Request, env: Env, id: string): Promise<Response> {
  if (!id || id.length > 128) {
    return jsonResponse({ error: "Invalid request" }, 400, { "Cache-Control": "no-store" });
  }
  if (!(await isAdminAuthorized(request, env))) {
    return jsonResponse({ error: "Unauthorized" }, 401, { "Cache-Control": "no-store" });
  }

  const result = await env.DB.prepare("DELETE FROM donations WHERE id = ?").bind(id).run();
  if (result.meta.changes === 0) {
    return jsonResponse({ error: "Not found" }, 404, { "Cache-Control": "no-store" });
  }
  return new Response(null, { status: 204, headers: { "Cache-Control": "no-store" } });
}

async function isAdminAuthorized(request: Request, env: Env): Promise<boolean> {
  const authorization = request.headers.get("Authorization");
  const providedToken = authorization?.startsWith("Bearer ")
    ? authorization.substring("Bearer ".length)
    : "";
  return Boolean(providedToken) && secretsEqual(providedToken, env.ADMIN_TOKEN);
}

async function readBodyWithinLimit(request: Request): Promise<Uint8Array | null> {
  const contentLength = Number(request.headers.get("Content-Length") ?? 0);
  if (Number.isFinite(contentLength) && contentLength > MAX_BODY_BYTES) return null;
  if (!request.body) return new Uint8Array();

  const reader = request.body.getReader();
  const chunks: Uint8Array[] = [];
  let totalBytes = 0;
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    totalBytes += value.byteLength;
    if (totalBytes > MAX_BODY_BYTES) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }

  const body = new Uint8Array(totalBytes);
  let offset = 0;
  for (const chunk of chunks) {
    body.set(chunk, offset);
    offset += chunk.byteLength;
  }
  return body;
}

function boundedRequiredString(value: unknown, maxLength: number): string | null {
  const normalized = stringValue(value)?.trim();
  if (!normalized || normalized.length > maxLength) return null;
  return normalized;
}

function boundedOptionalString(value: unknown, maxLength: number): string | null | undefined {
  if (value === null || value === undefined || value === "") return null;
  const normalized = stringValue(value)?.trim();
  if (normalized === undefined || normalized.length > maxLength) return undefined;
  return normalized || null;
}

function stringValue(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function normalizeTimestamp(value: unknown): string | null {
  const raw = boundedRequiredString(value, 80);
  if (!raw) return null;
  const parsed = new Date(raw);
  if (Number.isNaN(parsed.getTime())) return null;
  if (parsed.getTime() > Date.now() + 5 * 60 * 1000) return null;
  return parsed.toISOString();
}

function safeHttpsUrl(value: unknown): string | null | undefined {
  if (value === null || value === "") return null;
  if (typeof value !== "string" || value.length > 2048) return undefined;
  try {
    const parsed = new URL(value);
    if (parsed.protocol !== "https:" || parsed.username || parsed.password || parsed.hash) {
      return undefined;
    }
    return parsed.toString();
  } catch {
    return undefined;
  }
}

function safeAvatarUrl(value: unknown): string | null | undefined {
  const url = safeHttpsUrl(value);
  if (url === null || url === undefined) return url;
  return APPROVED_AVATAR_HOSTS.has(new URL(url).hostname.toLowerCase()) ? url : undefined;
}

async function secretsEqual(left: string, right: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [leftHash, rightHash] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(left)),
    crypto.subtle.digest("SHA-256", encoder.encode(right)),
  ]);
  const leftBytes = new Uint8Array(leftHash);
  const rightBytes = new Uint8Array(rightHash);
  let difference = 0;
  for (let index = 0; index < leftBytes.length; index += 1) {
    difference |= leftBytes[index] ^ rightBytes[index];
  }
  return difference === 0;
}

function methodNotAllowed(allow: string): Response {
  return jsonResponse({ error: "Method not allowed" }, 405, { Allow: allow });
}

function jsonResponse(
  body: unknown,
  status: number,
  headers: Record<string, string> = {},
): Response {
  return Response.json(body, {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...headers,
    },
  });
}
