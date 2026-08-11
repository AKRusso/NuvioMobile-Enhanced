# Ko-fi supporters worker

This Cloudflare Worker receives Ko-fi webhooks and exposes the public supporter
list consumed by NuvioMobile Enhanced.

## Privacy

Only public donations are stored. The database contains the public display name,
public message, donation date, and optional approved avatar/profile URLs. It does
not store email addresses, amounts, shipping data, shop items, transaction URLs,
or raw webhook payloads.

Ko-fi webhooks do not include profile photos. Use the authenticated admin endpoint
to attach an avatar URL after receiving the supporter's permission.

## Endpoints

- `POST /webhooks/kofi`: Ko-fi webhook.
- `GET /api/donations?view=recent`: public app API.
- `GET /api/contributors`: cached GitHub contributor list used by the app.
- `PATCH /api/admin/donations/:id`: set `avatar` and/or `profile` with an
  `Authorization: Bearer <ADMIN_TOKEN>` header.
- `DELETE /api/admin/donations/:id`: permanently remove a donation after a
  privacy request, using the same authorization header.
- `GET /healthz`: D1 health check.

Avatar images are limited to approved GitHub, Ko-fi, and Discord CDN hosts so an
arbitrary image server cannot track app users. Profile links may use any HTTPS URL
because they are opened only after an explicit user action.

## Setup

1. Install dependencies with `npm install`.
2. Create the D1 database and add its binding as `DB` in `wrangler.jsonc`.
3. Apply `migrations/0001_init.sql` with `npm run migrate:remote`.
4. Add `KOFI_VERIFICATION_TOKEN` and `ADMIN_TOKEN` using `wrangler secret put`.
5. Deploy with `npm run deploy`.
6. Configure Ko-fi to send webhooks to `/webhooks/kofi`.

Never commit either secret.
