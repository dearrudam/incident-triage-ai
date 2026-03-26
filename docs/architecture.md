# Arquitetura

## Módulos Maven

| Módulo | Porta padrão | Responsabilidade |
|---|---|---|
| `incident-triage-mvp` | 8080 | Recebe incidentes, gera embedding, busca histórico similar por vetor, decide prioridade via LLM |
| `servicenow-simulator` | 8081 | Simula a ServiceNow Table API; gera e despacha incidentes sintéticos via LLM |

Ambos os módulos herdam do POM raiz (`groupId: dev.dearrudam`, Java 25, Quarkus 3.34.1, LangChain4j 1.8.2).

## Padrão Arquitetural: Ports & Adapters

A estrutura de pacotes é idêntica nos dois módulos:

```
src/main/java/dev/dearrudam/{triage|simulator}/
  domain/
    model/      ← Records Java puros — ZERO imports de framework
    port/       ← Interfaces Java puras — ZERO imports de framework
  application/  ← CDI @ApplicationScoped — orquestra as portas
  adapter/
    in/rest/    ← JAX-RS @Path Resources (entrada HTTP)
    out/ai/     ← Implementações LangChain4j das portas de IA (saída)
    out/db/     ← Hibernate + native SQL com pgvector (saída — BD) [somente triage-mvp]
    out/rest/   ← MicroProfile REST Client (saída — HTTP) [somente simulator]
    store/      ← AtomicReference in-memory store [somente simulator]
```

## Restrições Arquiteturais Obrigatórias

1. **Domínio puro**: `domain/model/` e `domain/port/` não devem ter nenhum import de framework. Essa restrição é comentada explicitamente em cada arquivo de domínio.
2. **`@RegisterAiService`** (LangChain4j) apenas em `adapter/out/ai/`.
3. **`@Entity`** (JPA) apenas em `adapter/out/db/`.
4. **DTOs de REST client** são inner `record` dentro da classe `*Client` que os utiliza.
5. **Dimensão do embedding**: fixada em `vector(768)` no schema. Trocar `OLLAMA_EMBEDDING_MODEL` requer recriar a tabela `historical_incidents`.

## Fluxo de Triagem (incident-triage-mvp)

```
POST /triage
  → TriageResource (adapter/in/rest)
  → TriageService (application)
      → EmbeddingGeneratorPort → LangChain4jEmbeddingGeneratorAdapter → Ollama nomic-embed-text
      → HistoricalIncidentRepository (adapter/out/db) — top-5 cosine search via pgvector <=>
      → TriageDecisionPort → LangChain4jTriageDecisionAdapter → Ollama gemma3
  ← TriageResponse { priority, category, suggestedTeam, rationale }
```

## Fluxo de Geração e Despacho (servicenow-simulator)

```
POST /simulation/generate  →  SyntheticIncidentGeneratorPort → LangChain4j → Ollama gemma3
                           →  SimulatedIncidentStore (AtomicReference — substitui lista)

GET /api/now/table/incident →  SimulatedIncidentStore → envelope ServiceNow { "result": [...] }

POST /simulation/dispatch   →  SimulatedIncidentStore → TriageServiceClient (REST Client) → POST /triage
```

## Banco de Dados

- **PostgreSQL 16 + pgvector** (Docker: `pgvector/pgvector:pg16`).
- Extensão ativada por `init-scripts/01-init-vector.sql`.
- Schema gerenciado por Hibernate com **`drop-and-create`** (perfil MVP — dados perdidos no restart).
- `StartupDataLoader` re-insere 7 incidentes históricos de seed automaticamente no `StartupEvent`.
- Operações vetoriais usam **native SQL** via `EntityManager.createNativeQuery` — não JPQL.
- Serialização do vetor: `List<Float>` ↔ string `"[f1,f2,...fn]"` com `CAST(? AS vector)` na escrita.
