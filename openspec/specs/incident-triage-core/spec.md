## ADDED Requirements

### Requirement: System accepts a triage request via REST
The `incident-triage-mvp` service SHALL expose a `POST /triage` endpoint that accepts a JSON body describing an incident and returns a structured triage decision.

#### Scenario: Valid triage request returns a decision
- **WHEN** a client sends `POST /triage` with a valid JSON body containing `title` and `description`
- **THEN** the system returns HTTP 200 with a JSON body containing `priority`, `category`, `suggestedTeam`, and `rationale`

#### Scenario: Missing required fields returns 400
- **WHEN** a client sends `POST /triage` with a body missing `title` or `description`
- **THEN** the system returns HTTP 400 with a descriptive error message

---

### Requirement: System generates an embedding for incoming incidents
The service SHALL generate a vector embedding for the combined `title` and `description` of every triage request using the `EmbeddingGeneratorPort` interface.

#### Scenario: Embedding generated before vector search
- **WHEN** a valid `POST /triage` request is received
- **THEN** the system calls `EmbeddingGeneratorPort.generate(text)` before querying the vector store

---

### Requirement: System performs vector similarity search against historical incidents
The service SHALL query the `historical_incidents` table using the cosine distance operator (`<=>`) from pgvector to retrieve the top-N most similar incidents.

#### Scenario: Similar incidents retrieved for triage context
- **WHEN** an embedding has been generated for an incoming triage request
- **THEN** the system queries `historical_incidents` ordered by cosine distance and retrieves the top 5 results

#### Scenario: No historical incidents available
- **WHEN** the `historical_incidents` table is empty
- **THEN** the system proceeds to the LLM decision step with an empty similar-incidents list, and still returns a triage response

---

### Requirement: System produces a triage decision via LLM
The service SHALL call `TriageDecisionPort.decide(request, similarIncidents)` to produce a structured `TriageResponse` containing `priority`, `category`, `suggestedTeam`, and `rationale`.

#### Scenario: LLM decision produced with context
- **WHEN** similar historical incidents are available
- **THEN** the LLM prompt includes those incidents as context and the response reflects an informed decision

#### Scenario: LLM decision produced without context
- **WHEN** no similar historical incidents are found
- **THEN** the LLM produces a triage decision based solely on the incoming incident text

---

### Requirement: Historical incidents are seeded at startup
The service SHALL seed a small, fixed set of historical incidents (with pre-generated embeddings) into the `historical_incidents` table when the application starts, if the table is empty.

#### Scenario: Startup seeding on empty database
- **WHEN** the application starts and `historical_incidents` is empty
- **THEN** at least 5 historical incident records with valid embeddings are inserted

#### Scenario: No duplicate seeding on restart
- **WHEN** the application starts and `historical_incidents` already contains records
- **THEN** no duplicate records are inserted

---

### Requirement: Domain model is framework-independent
The domain model classes (`HistoricalIncident`, `TriageRequest`, `TriageResponse`) and port interfaces (`EmbeddingGeneratorPort`, `TriageDecisionPort`) SHALL reside in the `domain` package with no imports from Quarkus, LangChain4j, or any persistence framework.

#### Scenario: Domain compiles without framework dependencies
- **WHEN** the `domain` package is compiled in isolation
- **THEN** there are no compile-time dependencies on `io.quarkus`, `dev.langchain4j`, or `jakarta.persistence`
