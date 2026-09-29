# DigitalOcean-URLShortener
Service to shorten and return URLs

## Requirements

- Java 21
- Maven 3.9+

## Run (backend + frontend)

```bash
cd DigitalOcean-URLShortener
./run.sh
```

Or with Maven directly:

```bash
mvn spring-boot:run
```

Optional env vars for `./run.sh`:
- `PORT=8080` — HTTP port
- `SKIP_TESTS=true` — skip tests during build (default)

Open http://localhost:8080/

## Docker

```bash
docker build -t url-shortener .
docker run --rm -p 8080:8080 url-shortener
```

### Managed Postgres (`.env` is source of truth)

1. Copy and edit secrets (never commit `.env`):

```bash
cp .env.example .env
# set DB_HOST, DB_USERNAME, DB_PASSWORD, etc.
```

2. Run with Compose:

```bash
docker compose up -d --build
```

Or with plain Docker:

```bash
docker build -t url-shortener .
docker run -d --name url-shortener --restart unless-stopped \
  --env-file .env -p 80:8080 url-shortener
```

## API

### Create short URL

`POST /api/shorten`

```json
{ "longUrl": "https://example.com/path", "alias": "my-link" }
```

`alias` is optional. Response body:

```json
{
  "status": "SUCCESS",
  "errorMessage": null,
  "shortUrl": "http://localhost:8080/my-link",
  "alias": "my-link"
}
```

```json
{
  "status": "FAILURE",
  "errorMessage": "Alias already taken",
  "shortUrl": null,
  "alias": null
}
```

`status` is the `CreateURLResponse` enum: `SUCCESS` | `FAILURE`.

### Resolve short URL

`GET /{alias}` → `302` redirect to the long URL (cache first, then DB).

## Project layout

```
src/main/java/com/digitalocean/urlshortener/
  web/          # HTTP controllers + DTOs
  service/      # create + resolve logic
  store/        # cache + JPA UrlRepository
  model/        # Url entity (alias PK, longUrl, timeCreated, ttl)
public/         # HTML/JS form
docs/flows.md   # request flow diagrams
```

## Docs

- [Request flows](docs/flows.md) — create, resolve, and request lifecycle / data flow
