# Desenvolvimento

## Pré-requisitos

| Ferramenta | Versão |
|---|---|
| Java | 25 (`maven.compiler.release=25`) |
| Maven | 3.9+ |
| Docker / Docker Compose | Qualquer versão recente |
| Ollama | Rodando localmente em `http://localhost:11434` |
| Modelos Ollama | `nomic-embed-text` (embedding 768d) e `gemma3` (chat) |

## Infraestrutura Local

Sobe PostgreSQL 16 com pgvector:

```bash
docker compose up -d
```

A extensão `vector` é habilitada automaticamente por `init-scripts/01-init-vector.sql` no primeiro start do container.

## Build

```bash
# Build completo a partir da raiz
mvn clean verify -DskipTests

# Por módulo
cd incident-triage-mvp  && mvn clean verify -DskipTests
cd servicenow-simulator && mvn clean verify -DskipTests
```

## Executar em Dev Mode

```bash
# Serviço de triagem (porta 8080)
cd incident-triage-mvp && mvn quarkus:run -Dquarkus.http.port=8080

# Simulador ServiceNow (porta 8081)
cd servicenow-simulator && mvn quarkus:run -Dquarkus.http.port=8081
```

> **Atenção:** O DDL do banco é `drop-and-create` — todos os dados são apagados a cada restart do serviço de triagem. O `StartupDataLoader` re-insere os 7 incidentes históricos de seed automaticamente no startup.

## Variáveis de Ambiente

### incident-triage-mvp

| Variável | Padrão |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `incidenttriage` |
| `DB_USER` | `triage` |
| `DB_PASSWORD` | `triage` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` |
| `OLLAMA_CHAT_MODEL` | `gemma3` |

### servicenow-simulator

| Variável | Padrão |
|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_CHAT_MODEL` | `gemma3` |
| `TRIAGE_SERVICE_URL` | `http://localhost:8080` |

## Testes

Não existem testes automatizados (decisão intencional do MVP). As dependências `quarkus-junit` e `rest-assured` estão declaradas nos POMs para uso futuro.

O smoke test manual está documentado em `openspec/changes/archive/2026-03-26-bootstrap-incident-triage-mvp/tasks.md` (Task Group 12).

Quando testes forem adicionados, o padrão esperado é:
- `@QuarkusTest` para testes de integração.
- `rest-assured` para asserções na camada HTTP.
- Quarkus Dev Services para PostgreSQL em testes (container automático).

## Workflow OpenSpec

Novas funcionalidades seguem o fluxo:

1. `openspec propose` — descreve o que e por quê (gera `proposal.md`, `design.md`, `tasks.md`, `specs/`)
2. `openspec apply` — implementa task a task
3. `openspec archive` — arquiva após conclusão

Skills: `.github/skills/` | Prompts: `.github/prompts/`
