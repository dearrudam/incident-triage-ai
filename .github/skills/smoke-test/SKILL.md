---
name: smoke-test
description: Execute the full smoke test of the incident-triage-ai system. Use this skill whenever the user wants to run, execute, or validate the smoke test, start the full stack, test the end-to-end flow, verify the services are working, or run the manual test. Triggers on phrases like "run smoke test", "executar smoke test", "testar o sistema", "subir a infra", "iniciar os serviços", "validar o fluxo", "smoke test".
metadata:
  author: incident-triage-ai
  version: "1.0"
---

Execute the full end-to-end smoke test for the incident-triage-ai system.

This skill covers Task Group 12 from the bootstrap change: start infrastructure, start both apps, and validate the full triage flow with curl examples.

---

## Pre-requisites

Before starting, verify the following are available:

- Docker / Docker Compose running
- Ollama running at `http://localhost:11434` with models `nomic-embed-text` and `gemma3`
- Java 25 and Maven 3.9+ installed

Check Ollama:
```bash
curl -s http://localhost:11434/api/tags | grep -E 'nomic-embed-text|gemma3'
```

If models are missing, pull them:
```bash
ollama pull nomic-embed-text
ollama pull gemma3
```

---

## Step 1 — Start Infrastructure (12.1a)

Start PostgreSQL with pgvector in the background:

```bash
docker compose up -d
```

Wait a few seconds for the container to initialize, then confirm it is healthy:

```bash
docker compose ps
```

Expected: the `postgres` service shows `running` or `healthy`.

---

## Step 2 — Build Both Applications (prerequisite for run)

Build from the project root to ensure there are no compilation errors before starting:

```bash
mvn clean verify -DskipTests
```

---

## Step 3 — Start incident-triage-mvp on port 8080 (12.1b)

Start the triage service in a background terminal or a new shell session:

```bash
cd incident-triage-mvp && mvn quarkus:run -Dquarkus.http.port=8080
```

> **Note:** There is no `/q/health` endpoint. Wait for the log line `Listening on: http://0.0.0.0:8080` before proceeding.
>
> **Note:** The DDL is `drop-and-create` — every restart wipes and re-seeds the 7 historical incidents automatically via `StartupDataLoader`.

---

## Step 4 — Start servicenow-simulator on port 8081 (12.1c)

Start the simulator in another background terminal or shell session:

```bash
cd servicenow-simulator && mvn quarkus:run -Dquarkus.http.port=8081
```

> **Note:** Wait for `Listening on: http://0.0.0.0:8081` in the logs before proceeding.

---

## Step 5 — Generate synthetic incidents (12.2)

POST to the simulator to generate 3 synthetic incidents:

```bash
curl -s -X POST http://localhost:8081/simulation/generate \
  -H "Content-Type: application/json" \
  -d '{"count": 3, "domain": "infrastructure"}' | jq .
```

**Expected:** HTTP 200 with a JSON body confirming 3 incidents were stored in memory.

---

## Step 6 — Dispatch incidents to triage (12.3)

POST to dispatch all stored incidents to the triage service:

```bash
curl -s -X POST http://localhost:8081/simulation/dispatch \
  -H "Content-Type: application/json" | jq .
```

**Expected:** HTTP 200 with a JSON array where each element contains:
- `priority` — e.g., `"P1"`, `"P2"`, `"P3"`
- `category` — e.g., `"network"`, `"database"`, `"application"`
- `suggestedTeam` — name of the team to handle the incident
- `rationale` — LLM-generated explanation for the triage decision

---

## Step 7 — Verify stored incidents (12.4)

Retrieve the incidents from the simulator's ServiceNow-compatible endpoint:

```bash
curl -s http://localhost:8081/api/now/table/incident | jq .
```

**Expected:** HTTP 200 with `{ "result": [ ... ] }` where the array is non-empty and matches the 3 incidents generated in Step 5.

---

## Step 8 — Direct triage validation (bonus)

Test the triage service directly with a custom incident:

```bash
curl -s -X POST http://localhost:8080/triage \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Database connection pool exhausted",
    "description": "All database connections are in use. New requests are timing out after 30 seconds. Started after a deployment at 14:00."
  }' | jq .
```

**Expected:** HTTP 200 with a `TriageResponse` JSON:
```json
{
  "priority": "P1",
  "category": "database",
  "suggestedTeam": "DBA Team",
  "rationale": "..."
}
```

Validation error example (HTTP 400):
```bash
curl -s -X POST http://localhost:8080/triage \
  -H "Content-Type: application/json" \
  -d '{"title": "", "description": ""}' | jq .
```

**Expected:** HTTP 400.

---

## Summary checklist

| Step | Command | Expected result |
|---|---|---|
| Infrastructure | `docker compose up -d` | postgres container running |
| Build | `mvn clean verify -DskipTests` | BUILD SUCCESS |
| Start triage | `mvn quarkus:run` on port 8080 | Listening on 8080 |
| Start simulator | `mvn quarkus:run` on port 8081 | Listening on 8081 |
| Generate incidents | `POST /simulation/generate` | 3 incidents stored |
| Dispatch triage | `POST /simulation/dispatch` | Each incident has priority + category + team + rationale |
| List incidents | `GET /api/now/table/incident` | result array non-empty |
| Direct triage | `POST /triage` | valid TriageResponse |

---

## Troubleshooting

**Port already in use:** Kill the process with `lsof -ti:8080 | xargs kill` (or 8081).

**Ollama timeout / model not found:** Run `ollama list` to confirm models are available. Restart Ollama if needed.

**DB connection refused:** Check Docker with `docker compose ps`. If the container exited, run `docker compose up -d` again.

**Empty embedding / NullPointerException:** Ollama model `nomic-embed-text` may not be loaded. Run `ollama pull nomic-embed-text`.

**`jq` not installed:** Replace `| jq .` with `| python3 -m json.tool` or omit it.
