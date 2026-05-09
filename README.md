# Spring Boot AI + Ollama + React

Minimal full-stack starter with:
- Spring Boot backend using Spring AI (Ollama provider)
- Vite + React frontend
- Docker Compose for Ollama, backend, and frontend

## Project Layout

- `backend/` - Spring Boot API: `POST /api/completion` (single-shot), `POST /api/chat` + `GET /api/chat/{id}/messages` (in-memory conversation), `POST /api/search` (Tavily only, no LLM), `POST /api/fetch-url` (HTTP fetch only, no LLM)
- `frontend/` - Vite/React: **Home** (`/`), **Prompt** (`/prompt` → `/api/completion`), **Chat** (`/chat`)
- `docker-compose.yml` - Dev stack with hot reload

## Prerequisites

- Docker + Docker Compose

### GPU (optional, recommended on NVIDIA)

The `ollama` service requests an NVIDIA GPU via Compose `deploy.resources.reservations.devices` (see [Docker Compose GPU support](https://docs.docker.com/compose/how-tos/gpu-support/)). For that to work you need:

- **Windows:** current NVIDIA driver for your GPU (Docker Desktop uses it from WSL2).
- **Docker Desktop:** enable **WSL 2** integration for your distro, and in Docker Desktop settings turn on **GPU** support for containers (labels vary by version; see engine [GPU prerequisites](https://docs.docker.com/engine/containers/resource_constraints/#gpu)).

**1. Host (WSL)** — driver and GPU visible:

```bash
nvidia-smi
```

**2. Inside the Ollama container** — confirms Docker passed the GPU through:

```bash
docker exec ollama nvidia-smi
```

You should see the same GPU and, while a model is inferring, a process such as `/ollama` using VRAM.

**3. Ollama startup / inference logs** — when a model loads, look for inference backend lines (for example CUDA) rather than **only** CPU-only inference, for example:

- `inference compute` / `library=cpu` on a machine **with** a GPU usually means the container is **not** using the GPU (settings, failed reservation, or driver path).

See also **First AI / Ollama completion is slow** (below): the first request after startup is often much slower than the next ones, on both GPU and CPU.

If Compose fails to start because of GPU reservation, remove the `deploy:` block under `ollama` in `docker-compose.yml` to fall back to CPU.

## Ollama log lines (`[GIN]`)

Ollama’s HTTP server uses the **Go Gin** framework. Lines like:

```text
[GIN] 2026/05/07 - 22:59:42 | 200 | 54.498µs | 127.0.0.1 | HEAD     "/"
[GIN] 2026/05/07 - 22:59:42 | 200 | 807.656µs | 127.0.0.1 | GET "/api/tags"
```

are **access logs**: HTTP method, path, status `200`, duration, and client IP (`127.0.0.1` is **inside the container**, e.g. health checks).

- **`HEAD /`** — quick “is the server up?” probe (often from health checks).
- **`GET /api/tags`** — lists model tags Ollama knows about; also used by tooling / probes.

These are **cheap metadata** calls (microseconds in the log). **Generation** from this app hits **`POST /api/completion`** on the Spring backend (which then calls Ollama). Ollama’s own HTTP API also exposes routes such as **`POST /api/generate`** / **`POST /api/chat`** — those show up in `[GIN]` logs with a **much longer** duration while the model runs.

## Run Everything with Docker Compose

From the repo root:

```bash
docker compose up --build
```

Then open:
- Frontend UI: `http://localhost:5173` — routes: **`/`** home, **`/prompt`** single-shot, **`/chat`** multi-turn
- Backend (single-shot): `POST http://localhost:8080/api/completion`
- Backend (chat + memory): `POST http://localhost:8080/api/chat`, `GET http://localhost:8080/api/chat/{conversationId}/messages`
- Backend (Tavily smoke test): `POST http://localhost:8080/api/search`
- Backend (URL fetch smoke test): `POST http://localhost:8080/api/fetch-url`
- Ollama API: `http://localhost:11434`

The compose stack:
- starts Ollama
- pulls model `${OLLAMA_MODEL}` (default `qwen2.5:3b`)
- starts backend and frontend in dev mode

**Context length:** The backend sets Ollama **`num_ctx`** via Spring AI (`spring.ai.ollama.chat.options.num-ctx`), default **32768** tokens to match the Qwen2.5 3B GGUF `context_length` (see [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml)). Override with **`OLLAMA_NUM_CTX`** (Compose passes it into the `backend` service; see [`.env.example`](.env.example)). If inference **OOM**s or gets very slow, **lower** `OLLAMA_NUM_CTX` (e.g. **16384**).

**Why `qwen2.5:3b` by default:** Smaller models vary a lot in how well they honor Spring AI tool calling. In this stack, **Qwen 2.5 3B** behaved more reliably than the previous default when the chat endpoint offers tools such as Tavily **`web_search`** and **`fetch_url`** (fewer missed or malformed tool calls). Override `OLLAMA_MODEL` anytime if you prefer another tag.

## First AI / Ollama completion is slow

The **first** completion after Ollama starts (or after the model was unloaded) is often **noticeably slower** than the second and later calls. That is expected and is **not** mainly Spring Boot overhead.

Typical reasons:

- **Model load** — weights read from disk into **RAM** (CPU) or **VRAM** (GPU).
- **GPU** — CUDA / driver context, first-time kernel work, graphs warming up.
- **CPU** — first inference path and caches; large models on CPU can take tens of seconds to over a minute for the first reply.

Use a **second** prompt soon after the first to judge normal latency. The backend logs each `/api/completion` duration at **INFO** (Guava `Stopwatch`) so you can compare first vs follow-up timings in the Spring logs.

## Hot Reload Behavior

- Frontend: Vite dev server with bind mount + polling watcher
- Backend: `mvn spring-boot:run` + Spring DevTools with bind-mounted source

When you edit code on host, containers should auto-reload.

If **`npm install` on the host fails with `EACCES`** under `frontend/node_modules`, ownership may belong to the Docker user from Compose; fix with `sudo chown -R "$(id -u):$(id -g)" frontend/node_modules` (or remove `node_modules` and reinstall), or rely on **`docker compose`** to install deps inside the frontend container.

## Use a Different Model

The stack uses `OLLAMA_MODEL` for both:
- the `ollama-pull` helper container (downloads the model)
- the backend Spring AI chat model setting

The default is **`qwen2.5:3b`** (see above). If you switch to another model, expect tool-calling quality for **`web_search`** / **`fetch_url`** to vary; you may need to tune prompts or pick a model known for strong function/tool use.

### One-off run with a different model

```bash
OLLAMA_MODEL=llama3.1:8b docker compose up --build
```

### Persistent default via `.env`

Create a `.env` file in the project root:

```bash
OLLAMA_MODEL=qwen2.5:3b
```

Then run normally:

```bash
docker compose up --build
```

### Switch models later

If containers are already running, restart so the new `OLLAMA_MODEL` is used:

```bash
docker compose down
OLLAMA_MODEL=mistral docker compose up --build
```

## Chat API (multi-turn, server memory)

- **Memory:** Spring AI [`ChatMemory`](https://docs.spring.io/spring-ai/reference/1.0/api/chat-memory.html) + `MessageChatMemoryAdvisor` on a dedicated `ChatClient` bean. Stored **in the JVM** — **lost on backend restart** (not durable across deploys). For persistence later, use JDBC/Redis-backed memory from Spring AI.
- **`conversationId`:** Opaque string (use a UUID). Send the **same** id on each turn. Omit or send blank on first message to let the server assign one (the client UI always generates a UUID for clarity).
- **New chat:** UI **New chat** assigns a fresh UUID and clears the transcript; no `DELETE` API is required.

### Tavily web search (optional, chat only)

When **`TAVILY_API_KEY`** is set, the backend registers a Spring AI tool **`web_search`** (Tavily) and attaches it on every **`POST /api/chat`** turn so the model may call it when it decides web search is appropriate. The **`fetch_url`** tool is also registered on every chat turn (no extra API key): it performs an HTTPS GET server-side and returns visible page text (HTML normalized with **Jsoup**). **`POST /api/completion`** never includes these tools.

- **Secrets:** Put the key in a repo-root **`.env`** file (see [`.env.example`](.env.example)); **`.env` is gitignored**. Docker Compose passes **`TAVILY_API_KEY`** into the `backend` service. **Do not** commit real keys in `application.yml` / `application.properties`.
- **Local `mvn spring-boot:run` without Docker:** Spring does not read `.env` automatically — export `TAVILY_API_KEY` in your shell, use your IDE env from `.env`, or run the stack with **`docker compose`**.
- **Tuning (no secrets):** `tavily.max-results` and `tavily.search-depth` in [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) (defaults: `5`, `basic`). See Tavily docs for credit/latency tradeoffs.
- If **`TAVILY_API_KEY`** is unset, chat still works; **`web_search`** is not registered.
- **Direct search (no model):** `POST /api/search` with JSON `{"query":"..."}` returns `{ "query", "output" }` where `output` is the same formatted snippet block the tool uses. Without a key, the response is **503** with a JSON `error` field.

```bash
curl -s -X POST http://localhost:8080/api/search \
  -H "Content-Type: application/json" \
  -d '{"query":"Spring AI 1.0 release date"}' | jq .
```

### Fetch URL (`fetch_url` tool + exercise endpoint)

- **Chat tool:** On every **`POST /api/chat`** turn, the model may call **`fetch_url`** with a full **`https://...`** URL. Responses are size-capped; HTML is turned into plain text via **Jsoup** (scripts/styles removed). This is intended for **private demos** only (basic host blocking, not full SSRF protection).
- **Tuning:** [`backend/src/main/resources/application.yml`](backend/src/main/resources/application.yml) under `app.fetch-url`: `max-response-chars`, `max-download-bytes`, `connect-timeout-ms`, `read-timeout-ms`, `allow-http` (default `false`; set `true` if you need plain `http` for local targets), and `log-response-max-chars` (default **8192**: logs that many characters of the returned text at **INFO**; set **0** to log only the character count).
- **Direct fetch (no model):** `POST /api/fetch-url` with JSON `{"url":"https://..."}` returns `{ "url", "output" }` using the same logic as the tool (handy for curl checks).

```bash
curl -s -X POST http://localhost:8080/api/fetch-url \
  -H "Content-Type: application/json" \
  -d '{"url":"https://example.com"}' | jq .
```

### Chat curl examples

Use one UUID for all turns in the same conversation (generate with `uuidgen` or any UUID v4).

```bash
CID="550e8400-e29b-41d4-a716-446655440000"

curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"$CID\",\"message\":\"My name is Alex.\"}"

curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"$CID\",\"message\":\"What is my name?\"}"

curl -s "http://localhost:8080/api/chat/$CID/messages"
```

## API Example (single-shot completion)

```bash
curl -X POST http://localhost:8080/api/completion \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Explain Spring AI in one paragraph."}'
```
