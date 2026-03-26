## ADDED Requirements

### Requirement: Simulator exposes a ServiceNow-compatible incident list endpoint
The `servicenow-simulator` service SHALL expose `GET /api/now/table/incident` that returns a JSON response envelope compatible with the ServiceNow Table API format, containing a list of simulated incident records.

#### Scenario: List endpoint returns incident records
- **WHEN** a client sends `GET /api/now/table/incident`
- **THEN** the system returns HTTP 200 with a JSON body `{ "result": [ ...incidents ] }` where each incident has at minimum `sys_id`, `short_description`, and `description`

#### Scenario: List endpoint returns empty result when no incidents generated
- **WHEN** no incidents have been generated yet
- **THEN** the system returns HTTP 200 with `{ "result": [] }`

---

### Requirement: Simulator exposes an endpoint to generate synthetic incidents
The simulator SHALL expose `POST /simulation/generate` that uses `SyntheticIncidentGeneratorPort` to generate a configurable number of synthetic incidents and store them in memory.

#### Scenario: Generate with explicit count and domain
- **WHEN** a client sends `POST /simulation/generate` with body `{ "count": 5, "domain": "database" }`
- **THEN** the system generates 5 synthetic incidents related to the `database` domain and stores them in the in-memory store

#### Scenario: Generate replaces previously stored incidents
- **WHEN** `POST /simulation/generate` is called a second time
- **THEN** previously generated incidents are replaced with the new batch

#### Scenario: Default domain used when not specified
- **WHEN** `POST /simulation/generate` is called without a `domain` field
- **THEN** the system uses a sensible default domain (e.g., `"general"`)

---

### Requirement: Simulator exposes an endpoint to dispatch incidents to the triage service
The simulator SHALL expose `POST /simulation/dispatch` that iterates all in-memory synthetic incidents and POSTs each one to the `incident-triage-mvp`'s `/triage` endpoint, collecting responses.

#### Scenario: Dispatch sends all stored incidents
- **WHEN** a client calls `POST /simulation/dispatch` and there are 3 incidents in the store
- **THEN** the system makes 3 sequential `POST /triage` calls to the triage service and returns a summary response including each triage result

#### Scenario: Dispatch with no incidents returns empty summary
- **WHEN** `POST /simulation/dispatch` is called and the in-memory store is empty
- **THEN** the system returns HTTP 200 with an empty results list and dispatched count of 0

---

### Requirement: Simulator AI abstraction for synthetic incident generation is port-based
The `SyntheticIncidentGeneratorPort` interface SHALL reside in the domain layer and generate synthetic incidents via an LLM. The LangChain4j implementation SHALL live in the adapter layer only.

#### Scenario: Synthetic incident structure matches domain model
- **WHEN** `SyntheticIncidentGeneratorPort.generate(count, domain)` is called
- **THEN** each returned `SimulatedIncident` has a non-empty `title`, `description`, and `domain`
