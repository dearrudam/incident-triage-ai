## 1. Repository & Project Structure

- [x] 1.1 Create a Maven multi-module parent POM at the repository root with modules `incident-triage-mvp` and `servicenow-simulator`
- [x] 1.2 Scaffold the `incident-triage-mvp` Quarkus project using the Quarkus Maven plugin (REST, Hibernate ORM with Panache, PostgreSQL JDBC, LangChain4j Ollama)
- [x] 1.3 Scaffold the `servicenow-simulator` Quarkus project using the Quarkus Maven plugin (REST, LangChain4j Ollama)
- [x] 1.4 Verify both projects build successfully with `mvn clean verify -DskipTests`

## 2. Local Infrastructure

- [x] 2.1 Create `docker-compose.yml` at the project root using the `ankane/pgvector` Docker image
- [x] 2.2 Configure the Docker Compose service with default credentials (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` environment-variable overrides)
- [x] 2.3 Add an init SQL script (or entrypoint command) that runs `CREATE EXTENSION IF NOT EXISTS vector;`
- [x] 2.4 Verify `docker compose up` starts cleanly and pgvector extension is accessible via `psql`

## 3. Domain Model — incident-triage-mvp

- [x] 3.1 Create `domain/model/HistoricalIncident.java` with fields: `id` (UUID), `title` (String), `description` (String), `priority` (String), `category` (String), `resolvedBy` (String), `embedding` (List\<Float\>)
- [x] 3.2 Create `domain/model/TriageRequest.java` with fields: `title` (String), `description` (String)
- [x] 3.3 Create `domain/model/TriageResponse.java` with fields: `priority` (String), `category` (String), `suggestedTeam` (String), `rationale` (String)
- [x] 3.4 Verify all three domain model classes compile with **no** imports from `io.quarkus`, `dev.langchain4j`, or `jakarta.persistence`

## 4. AI Port Interfaces — incident-triage-mvp

- [x] 4.1 Create `domain/port/EmbeddingGeneratorPort.java` with method `List<Float> generate(String text)`
- [x] 4.2 Create `domain/port/TriageDecisionPort.java` with method `TriageResponse decide(TriageRequest request, List<HistoricalIncident> similarIncidents)`
- [x] 4.3 Verify both interfaces are in the `domain` package with no framework dependencies

## 5. Persistence Adapter — incident-triage-mvp

- [x] 5.1 Create a Hibernate entity `adapter/out/db/HistoricalIncidentEntity.java` mapping to the `historical_incidents` table with a `vector` column for the embedding
- [x] 5.2 Configure `application.properties` with datasource pointing to Docker Compose Postgres, `quarkus.hibernate-orm.database.generation=drop-and-create`, and pgvector dialect support
- [x] 5.3 Implement a repository method that queries `historical_incidents` ordered by cosine distance (`<=>`) and returns the top 5 results as `HistoricalIncident` domain objects
- [x] 5.4 Verify the table is created on application startup with no schema errors

## 6. AI Adapters — incident-triage-mvp

- [x] 6.1 Create `adapter/out/ai/LangChain4jEmbeddingGeneratorAdapter.java` implementing `EmbeddingGeneratorPort` using the LangChain4j embedding model bean
- [x] 6.2 Create `adapter/out/ai/LangChain4jTriageDecisionAdapter.java` implementing `TriageDecisionPort` using a LangChain4j `AiService` interface with a structured prompt
- [x] 6.3 Configure `application.properties` for Ollama base URL, embedding model name, and chat model name

## 7. Application Layer & Startup Seeding — incident-triage-mvp

- [x] 7.1 Create `application/TriageService.java` that orchestrates: call `EmbeddingGeneratorPort`, query vector store, call `TriageDecisionPort`, return `TriageResponse`
- [x] 7.2 Create a startup event listener (`@Observes StartupEvent`) `StartupDataLoader.java` that seeds at least 5 `HistoricalIncident` records (with embeddings) if the table is empty
- [x] 7.3 Verify the seeding logic does not insert duplicates on restart

## 8. REST Adapter — incident-triage-mvp

- [x] 8.1 Create `adapter/in/rest/TriageResource.java` exposing `POST /triage` consuming and producing `application/json`
- [x] 8.2 Map `TriageRequest` from the request body and return `TriageResponse` as the response body
- [x] 8.3 Return HTTP 400 when `title` or `description` is missing or blank

## 9. Domain Model — servicenow-simulator

- [x] 9.1 Create `domain/model/SimulatedIncident.java` with fields: `title` (String), `description` (String), `domain` (String)
- [x] 9.2 Create `domain/port/SyntheticIncidentGeneratorPort.java` with method `List<SimulatedIncident> generate(int count, String domain)`

## 10. AI Adapter & In-Memory Store — servicenow-simulator

- [x] 10.1 Create `adapter/out/ai/LangChain4jSyntheticIncidentGeneratorAdapter.java` implementing `SyntheticIncidentGeneratorPort` using a LangChain4j `AiService` with a structured generation prompt
- [x] 10.2 Create an `@ApplicationScoped` `SimulatedIncidentStore.java` bean that holds the current list of `SimulatedIncident` in memory (replaces entire list on each generate call)
- [x] 10.3 Configure `application.properties` for Ollama base URL and chat model name

## 11. REST Adapters — servicenow-simulator

- [x] 11.1 Create `adapter/in/rest/ServiceNowTableResource.java` exposing `GET /api/now/table/incident` returning `{ "result": [...] }` from the in-memory store
- [x] 11.2 Create `adapter/in/rest/SimulationResource.java` exposing `POST /simulation/generate` (accepts `{ "count": N, "domain": "..." }`, calls `SyntheticIncidentGeneratorPort`, stores results)
- [x] 11.3 Implement `POST /simulation/dispatch` in `SimulationResource.java` that iterates stored incidents, POSTs each to `incident-triage-mvp`'s `/triage` using an injected REST client, and returns a summary
- [x] 11.4 Configure the triage service base URL via `application.properties` with an environment-variable override (`TRIAGE_SERVICE_URL`)

## 12. End-to-End Smoke Test

- [x] 12.1 Start the Docker Compose stack and both applications in dev mode
- [x] 12.2 Call `POST /simulation/generate` with `count=3` and verify 3 incidents are stored
- [x] 12.3 Call `POST /simulation/dispatch` and verify each incident is triaged and a `priority`, `category`, `suggestedTeam`, and `rationale` are returned for each
- [x] 12.4 Call `GET /api/now/table/incident` and verify the `result` array is non-empty and matches the generated incidents
