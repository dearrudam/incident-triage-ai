## ADDED Requirements

### Requirement: PostgreSQL with pgvector runs via Docker Compose
The repository SHALL include a `docker-compose.yml` at the project root that starts a PostgreSQL instance with the pgvector extension available and pre-enabled.

#### Scenario: pgvector extension is available after container start
- **WHEN** `docker compose up` completes successfully
- **THEN** a connection to the database can execute `SELECT * FROM pg_extension WHERE extname = 'vector'` and return at least one row

#### Scenario: Database is accessible on the standard port
- **WHEN** the Docker Compose stack is running
- **THEN** PostgreSQL is reachable at `localhost:5432` with credentials defined in `docker-compose.yml`

---

### Requirement: Application configuration connects to the local database
Both `incident-triage-mvp` and `servicenow-simulator` SHALL include an `application.properties` (or `application.yml`) that configures the JDBC datasource to connect to the Docker Compose PostgreSQL instance using clearly documented default credentials.

#### Scenario: Triage application connects to database on startup
- **WHEN** `incident-triage-mvp` starts with the Docker Compose stack running
- **THEN** Hibernate ORM initialises without errors and the `historical_incidents` table is created

#### Scenario: Config uses environment-variable overrides
- **WHEN** environment variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, or `DB_PASSWORD` are set
- **THEN** the application uses those values instead of the defaults, without code changes
