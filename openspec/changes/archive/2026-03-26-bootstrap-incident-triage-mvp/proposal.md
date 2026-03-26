## Why

The project currently lacks any runnable foundation. We need a minimal, working MVP that demonstrates the full incident triage flow end-to-end — from incident ingestion through embedding generation, vector similarity search, and LLM-based triage decision — so the team can validate the concept and iterate quickly.

## What Changes

- Introduce the `incident-triage-mvp` application: the core service responsible for receiving triage requests, generating embeddings, performing vector search against historical incidents, and producing a triage decision via an LLM.
- Introduce the `servicenow-simulator` application: a lightweight service that mimics a ServiceNow-like REST API and can generate and dispatch synthetic incidents to the triage service.
- Add a `docker-compose.yml` for local development with PostgreSQL + pgvector.
- Define the initial domain model shared across the system.
- Define AI abstraction interfaces (ports) to decouple the domain from any specific AI framework.
- Define minimal REST endpoints for both applications.

## Capabilities

### New Capabilities

- `incident-triage-core`: Core triage service — receives a triage request, generates an embedding, searches for similar historical incidents via pgvector, and calls an LLM to produce a structured triage decision.
- `servicenow-simulator`: Simulator service — exposes a ServiceNow-compatible `/api/now/table/incident` endpoint and provides `/simulation/generate` and `/simulation/dispatch` endpoints for synthetic incident generation and dispatching.
- `local-infrastructure`: Docker Compose setup providing a local PostgreSQL instance with the pgvector extension enabled.

### Modified Capabilities

## Impact

- **New Projects**: `incident-triage-mvp` (Java/Quarkus), `servicenow-simulator` (Java/Quarkus)
- **Infrastructure**: `docker-compose.yml` at the repository root
- **Domain Layer**: New domain model classes (`HistoricalIncident`, `TriageRequest`, `TriageResponse`, `SimulatedIncident`) and AI port interfaces (`EmbeddingGeneratorPort`, `TriageDecisionPort`, `SyntheticIncidentGeneratorPort`)
- **No production concerns** (no auth, no retry logic, no observability) in this MVP
