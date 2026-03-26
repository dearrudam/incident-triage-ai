# Incident Triage AI

Sistema de triagem de incidentes com IA, construído com **Quarkus**, **LangChain4j** e **Ollama**. Ele classifica automaticamente incidentes de TI recebidos por prioridade, categoria e equipe responsável usando uma combinação de **busca por similaridade vetorial** (pgvector) sobre incidentes históricos e um **LLM local** para tomada de decisão estruturada.

## Visão Geral da Arquitetura

```
┌─────────────────────────────┐        ┌─────────────────────────────┐
│     servicenow-simulator    │        │      incident-triage-mvp    │
│         (port 8081)         │───────▶│          (port 8080)        │
│                             │        │                             │
│  GET  /api/now/table/       │        │  POST /triage               │
│         incident            │        │                             │
│  POST /simulation/generate  │        │  1. Generate embedding      │
│  POST /simulation/dispatch  │        │     (nomic-embed-text)      │
└─────────────────────────────┘        │  2. Vector search           │
                                       │     (pgvector cosine)       │
                                       │  3. LLM triage decision     │
                                       │     (gemma3)                │
                                       └──────────────┬──────────────┘
                                                      │
                                       ┌──────────────▼──────────────┐
                                       │  PostgreSQL + pgvector      │
                                       │  (historical_incidents)     │
                                       └─────────────────────────────┘
```

O projeto segue uma **Arquitetura Hexagonal** (Ports & Adapters): a camada de domínio tem dependência zero de frameworks ou bibliotecas de IA, e toda integração acontece por meio de portas de interface implementadas por adaptadores.

## Módulos

| Módulo | Descrição |
|---|---|
| `incident-triage-mvp` | Serviço principal de triagem. Recebe incidentes, gera embeddings, busca incidentes históricos similares e produz uma decisão estruturada de triagem com IA. |
| `servicenow-simulator` | Simulador leve que expõe uma API REST compatível com ServiceNow. Pode gerar incidentes sintéticos via LLM e enviá-los ao serviço de triagem. |

## Stack Tecnológica

- **Java 25** + **Quarkus 3.34.1**
- **Quarkus LangChain4j 1.8.2** (Ollama integration)
- **Ollama** — runtime local de LLM
  - `nomic-embed-text` — modelo de embedding (768 dimensões)
  - `gemma3` — modelo de chat/raciocínio
- **PostgreSQL 16** + **pgvector** — busca por similaridade vetorial
- **Hibernate ORM** com queries SQL nativas para pgvector
- **JAX-RS** para endpoints REST

## Pré-requisitos

- Java 25+
- Maven 3.9+
- Docker e Docker Compose
- [Ollama](https://ollama.com) rodando localmente com os modelos necessários:

```bash
ollama pull nomic-embed-text
ollama pull gemma3
```

## Como Executar

### 1. Subir a Infraestrutura

```bash
docker compose up -d
```

Isso inicia uma instância PostgreSQL 16 com a extensão pgvector habilitada na porta `5432`.

### 2. Executar o Serviço de Triagem

```bash
cd incident-triage-mvp
mvn quarkus:run
```

O serviço sobe em **http://localhost:8080**. Na primeira inicialização, ele popula o banco com um pequeno conjunto de incidentes históricos.

### 3. Executar o Simulador ServiceNow

```bash
cd servicenow-simulator
mvn quarkus:run
```

O simulador sobe em **http://localhost:8081**.

## Smoke Test E2E

O smoke test valida o fluxo completo de triagem: geração de incidentes sintéticos, dispatch ao serviço de triagem e verificação das respostas. Execute os passos abaixo para validar que o sistema está funcionando corretamente.

### Pré-requisitos

- Docker Compose em execução: `docker compose up -d`
- Serviço de triagem rodando: `cd incident-triage-mvp && mvn quarkus:run`
- Simulador ServiceNow rodando: `cd servicenow-simulator && mvn quarkus:run`
- `curl` ou similar para fazer requisições HTTP

### Passo 1: Gerar Incidentes Sintéticos

Gera 3 incidentes sintéticos no domínio de rede e os armazena em memória no simulador:

```bash
curl -X POST http://localhost:8081/simulation/generate \
  -H "Content-Type: application/json" \
  -d '{
    "count": 3,
    "domain": "network"
  }'
```

**Resposta esperada:**
```json
{
  "generated": 3,
  "domain": "network"
}
```

### Passo 2: Verificar Incidentes no Simulador

Recupera os incidentes sintéticos armazenados em memória:

```bash
curl -X GET http://localhost:8081/api/now/table/incident
```

**Resposta esperada:** Um array `result` com 3 incidentes contendo `sys_id`, `short_description`, `description`, etc.

```json
{
  "result": [
    {
      "sys_id": "...",
      "short_description": "...",
      "description": "..."
    },
    ...
  ]
}
```

### Passo 3: Despachar Incidentes para Triagem

Envia todos os incidentes armazenados ao serviço de triagem, que realiza embedding, busca por similaridade e produz uma decisão de triagem estruturada:

```bash
curl -X POST http://localhost:8081/simulation/dispatch
```

**Resposta esperada:** Um array `results` com um objeto para cada incidente contendo `priority`, `category`, `suggestedTeam` e `rationale`:

```json
{
  "dispatched": 3,
  "results": [
    {
      "incident": "BGP session flapping on core router",
      "priority": "CRITICAL",
      "category": "Network",
      "suggestedTeam": "Network Ops",
      "rationale": "..."
    },
    ...
  ]
}
```

### Checklist de Validação

- [ ] `POST /simulation/generate` retorna status 2xx e `generated: 3`
- [ ] `GET /api/now/table/incident` retorna status 2xx com array `result` contendo 3 itens
- [ ] Cada item em `result` tem `sys_id`, `short_description` e `description` não-vazios
- [ ] `POST /simulation/dispatch` retorna status 2xx e `dispatched: 3`
- [ ] Cada objeto em `results` contém `priority`, `category`, `suggestedTeam` e `rationale` não-vazios
- [ ] Os valores de `priority` estão dentro de `[CRITICAL, HIGH, MEDIUM, LOW]`

Se todos os itens acima forem validados, o smoke test passou ✅

## Referência da API

### Serviço de Triagem — `incident-triage-mvp` (porta 8080)

#### `POST /triage`

Realiza a triagem de um incidente recebido e retorna uma decisão estruturada.

**Requisição:**
```json
{
  "title": "Database connection pool exhausted",
  "description": "The application cannot acquire new DB connections. Response times have degraded significantly."
}
```

**Resposta:**
```json
{
  "priority": "HIGH",
  "category": "Database",
  "suggestedTeam": "DBA Team",
  "rationale": "Symptoms match a known connection pool exhaustion pattern. Immediate DBA intervention required to restore service."
}
```

Valores possíveis para prioridade: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`

---

### Simulador ServiceNow — `servicenow-simulator` (porta 8081)

#### `GET /api/now/table/incident`

Retorna os incidentes sintéticos armazenados em um envelope compatível com o formato do ServiceNow.

**Resposta:**
```json
{
  "result": [
    {
      "sys_id": "...",
      "short_description": "...",
      "description": "...",
      "category": "..."
    }
  ]
}
```

#### `POST /simulation/generate`

Gera incidentes sintéticos usando o LLM e os armazena em memória.

**Requisição:**
```json
{
  "count": 3,
  "domain": "network"
}
```

**Resposta:**
```json
{
  "generated": 3,
  "domain": "network"
}
```

#### `POST /simulation/dispatch`

Envia todos os incidentes sintéticos armazenados ao serviço de triagem e retorna os resultados.

**Resposta:**
```json
{
  "dispatched": 3,
  "results": [
    {
      "incident": "BGP session flapping on core router",
      "priority": "CRITICAL",
      "category": "Network",
      "suggestedTeam": "Network Ops",
      "rationale": "..."
    }
  ]
}
```

## Como o Fluxo de Triagem Funciona

1. **Geração de Embedding** — o título e a descrição do incidente são concatenados e enviados ao `nomic-embed-text` via Ollama para produzir um vetor de embedding com 768 dimensões.
2. **Busca por Similaridade Vetorial** — o embedding é comparado com todos os incidentes históricos armazenados no PostgreSQL usando o operador de distância cosseno do pgvector (`<=>`). Os 5 incidentes mais similares são recuperados.
3. **Decisão via LLM** — o `gemma3` recebe os detalhes do incidente junto com os incidentes históricos similares como contexto e produz uma resposta JSON estruturada contendo `priority`, `category`, `suggestedTeam` e `rationale`.

## Configuração

### `incident-triage-mvp`

| Propriedade | Padrão | Descrição |
|---|---|---|
| `DB_HOST` | `localhost` | Host do PostgreSQL |
| `DB_PORT` | `5432` | Porta do PostgreSQL |
| `DB_NAME` | `incidenttriage` | Nome do banco |
| `DB_USER` | `triage` | Usuário do banco |
| `DB_PASSWORD` | `triage` | Senha do banco |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL base do Ollama |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Modelo de embedding |
| `OLLAMA_CHAT_MODEL` | `gemma3` | Modelo de chat/raciocínio |

### `servicenow-simulator`

| Propriedade | Padrão | Descrição |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | URL base do Ollama |
| `OLLAMA_CHAT_MODEL` | `gemma3` | Modelo de chat para geração de incidentes |
| `TRIAGE_SERVICE_URL` | `http://localhost:8080` | URL do serviço de triagem |

> **Observação:** Alterar o modelo de embedding exige recriar a tabela `historical_incidents`, porque a dimensão do vetor precisa continuar compatível. O modelo padrão (`nomic-embed-text`) produz vetores com 768 dimensões.

## Estrutura do Projeto

```
incident-triage-ai/
├── docker-compose.yml              # PostgreSQL local + pgvector
├── init-scripts/
│   └── 01-init-vector.sql          # Habilita a extensão pgvector
├── openspec/                       # Propostas de mudança, specs e arquivos arquivados
│   ├── config.yaml
│   ├── changes/
│   │   └── archive/                # Mudanças concluídas e arquivadas
│   └── specs/                      # Especificações ativas
├── incident-triage-mvp/            # Serviço principal de triagem
│   └── src/main/java/dev/dearrudam/triage/
│       ├── domain/
│       │   ├── model/              # Records de domínio puros (sem dependência de framework)
│       │   └── port/               # Interfaces de porta para IA e persistência
│       ├── application/            # TriageService, StartupDataLoader
│       └── adapter/
│           ├── in/rest/            # Adaptador REST de entrada (TriageResource)
│           └── out/
│               ├── ai/             # Adaptadores LangChain4j (embedding + triagem)
│               └── db/             # Adaptadores PostgreSQL + pgvector
└── servicenow-simulator/           # Simulador ServiceNow + ferramenta de dispatch
    └── src/main/java/dev/dearrudam/simulator/
        ├── domain/
    │   ├── model/              # SimulatedIncident
    │   └── port/               # SyntheticIncidentGeneratorPort
        └── adapter/
      ├── in/rest/            # Adaptadores REST de ServiceNow e simulação
      ├── out/ai/             # Geração de incidentes sintéticos com LangChain4j
      ├── out/rest/           # Cliente REST para o serviço de triagem
      └── store/              # Armazenamento em memória dos incidentes
```

## Limitações do MVP

Este MVP é focado em demonstrar o fluxo fim a fim. As limitações abaixo são conhecidas e intencionais nesta fase:

- **Gerenciamento de schema**: usa `drop-and-create`, o que não é adequado para produção. Substitua por Flyway ou Liquibase antes de fazer deploy.
- **Sem autenticação ou autorização** em qualquer endpoint.
- **Sem lógica de retry** ou circuit breakers para Ollama ou para o serviço de triagem.
- **Sem observabilidade** (métricas, tracing, logging estruturado).
- **Store de incidentes em memória** no simulador, com perda de estado a cada restart.
