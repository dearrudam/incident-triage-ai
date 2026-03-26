## Context

There is currently no runnable code in this repository. The goal is to bootstrap the minimal project structure that proves the core hypothesis: given an incoming incident, the system can find similar historical incidents via vector search and produce a triage decision using an LLM — all running locally with no cloud dependencies.

Two applications are needed:
- `incident-triage-mvp`: the core intelligence service.
- `servicenow-simulator`: a test harness that mimics ServiceNow's incident API and can generate/dispatch synthetic incidents.

The system must be runnable from a single `docker-compose up` for PostgreSQL, with both applications started independently.

## Goals / Non-Goals

**Goals:**
- Establish a working multi-module project structure (Maven or Gradle multi-project).
- Define a minimal ports-and-adapters architecture that decouples domain logic from AI frameworks and persistence.
- Provide a complete end-to-end flow: HTTP request → embedding → vector search → LLM decision → response.
- Run locally with Docker Compose (PostgreSQL + pgvector).
- Define all domain models and AI port interfaces.
- Define all minimal REST endpoints.

**Non-Goals:**
- Authentication, authorization, or API keys management.
- Retry logic, circuit breakers, or resilience patterns.
- Observability, metrics, or distributed tracing.
- Multi-tenant support.
- Production deployment configuration.
- Automated test coverage beyond a smoke test.

## Decisions

### D1 — Runtime: Quarkus

**Decision**: Both applications use Quarkus as the runtime framework.

**Rationale**: Quarkus provides fast startup, native CDI, built-in REST (RESTEasy Reactive), Hibernate ORM with Panache, and first-class LangChain4j integration via `quarkus-langchain4j`. It avoids the overhead of Spring Boot for an exploratory MVP.

**Alternatives considered**: Spring Boot — ruled out due to heavier setup overhead and less direct LangChain4j integration in the Quarkus ecosystem.

---

### D2 — Project Layout: Multi-module Maven

**Decision**: A Maven multi-module project with modules `incident-triage-mvp` and `servicenow-simulator` under a common parent POM.

**Rationale**: Keeps both applications in a single repository with shared dependency management while keeping build and runtime boundaries clear.

**Alternatives considered**: Independent projects — rejected because it complicates local development setup.

---

### D3 — Architecture Pattern: Ports and Adapters (Hexagonal)

**Decision**: Apply a simplified ports-and-adapters style within each application.

**Structure per application:**
```
src/main/java/.../
  domain/          # Pure domain model — no framework dependencies
    model/         # HistoricalIncident, TriageRequest, TriageResponse, SimulatedIncident
    port/          # Java interfaces: EmbeddingGeneratorPort, TriageDecisionPort, SyntheticIncidentGeneratorPort
  application/     # Use-case orchestrators (call ports, coordinate domain objects)
  adapter/
    in/rest/       # REST resources (Quarkus/JAX-RS)
    out/ai/        # LangChain4j implementations of AI ports
    out/db/        # Hibernate/Panache persistence adapters
```

**Rationale**: Isolates the domain from LangChain4j and Quarkus, making it testable without infrastructure and replaceable as the project matures.

**Alternatives considered**: Transaction Script style — rejected as it would tangle AI framework concerns directly into REST handlers.

---

### D4 — AI Abstraction: Port Interfaces

**Decision**: The domain layer exposes three port interfaces:

- `EmbeddingGeneratorPort`: `List<Float> generate(String text)`
- `TriageDecisionPort`: `TriageResponse decide(TriageRequest request, List<HistoricalIncident> similarIncidents)`
- `SyntheticIncidentGeneratorPort`: `List<SimulatedIncident> generate(int count, String domain)`

LangChain4j implementations live in `adapter/out/ai/` only. The domain and application layers import only the interfaces.

**Rationale**: Zero leakage of AI framework APIs into domain logic. Swapping models or frameworks requires changing only the adapter.

---

### D5 — Vector Storage: PostgreSQL + pgvector via Hibernate

**Decision**: Use the `pgvector` PostgreSQL extension. Store embeddings as a `vector` column in the `historical_incidents` table. Query via native SQL `<=>` cosine distance operator.

**Rationale**: Avoids introducing a dedicated vector database for an MVP. PostgreSQL with pgvector is production-capable for moderate scale.

**Alternatives considered**: In-memory cosine search — rejected as it does not reflect the actual target architecture.

---

### D6 — Embedding & LLM Provider: Ollama (local)

**Decision**: Use Ollama as the local model provider for both embedding generation and LLM triage decisions, configured via `quarkus-langchain4j-ollama`.

**Rationale**: Zero cost, zero cloud dependency, works offline. Models can be swapped by changing the Quarkus config.

**Alternatives considered**: OpenAI — viable but requires an API key and internet access, inappropriate for a fully local MVP.

---

### D7 — ServiceNow Simulator Design

**Decision**: The simulator exposes:
- `GET /api/now/table/incident` — returns a list of fake incident records (mimics ServiceNow's Table API response envelope).
- `POST /simulation/generate` — triggers LLM-based synthetic incident generation, stores results in memory.
- `POST /simulation/dispatch` — iterates stored synthetic incidents and POSTs each to `incident-triage-mvp`'s `/triage` endpoint.

**Rationale**: Decouples simulation control from the ServiceNow-compatible endpoint. The triage service only needs to know about `/triage`; the simulator is purely a test-data producer.

---

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| Ollama model not available at startup | Log a clear error; do not crash — port interfaces allow the triage logic to surface a graceful error response |
| pgvector extension not enabled | `docker-compose.yml` uses `ankane/pgvector` image; startup SQL ensures extension is created |
| Embedding dimensions mismatch between stored and queried vectors | Fix embedding model in config; add a comment warning that changing the model requires table migration |
| Simulator dispatching incidents faster than MVP can handle | Not mitigated in MVP — fire-and-forget with sequential dispatch is acceptable |
| No schema migration tool | Use Hibernate `quarkus.hibernate-orm.database.generation=drop-and-create` for MVP; note that production will need Flyway/Liquibase |

## Open Questions

- Should `HistoricalIncident` embeddings be pre-seeded via a data loader at startup, or loaded when the first `/triage` request arrives?
  - **MVP decision**: seed a small fixed dataset at application startup using an `@ApplicationScoped` startup event.
- Should the simulator store synthetic incidents in-memory or in a local file?
  - **MVP decision**: in-memory (application-scoped bean), reset on restart.
