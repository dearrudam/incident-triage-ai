# Convenções

## Nomeação de Classes

| Elemento | Padrão | Exemplo |
|---|---|---|
| Modelo de domínio | Java `record`, sem sufixo | `TriageRequest`, `SimulatedIncident` |
| Porta | Interface `*Port` | `EmbeddingGeneratorPort`, `TriageDecisionPort` |
| Adaptador de IA | `LangChain4j*Adapter` | `LangChain4jEmbeddingGeneratorAdapter` |
| Serviço IA (LangChain4j) | `*AiService` + `@RegisterAiService` | `TriageDecisionAiService` |
| Resource JAX-RS | `*Resource` | `TriageResource`, `SimulationResource` |
| Repositório Hibernate | `*Repository` | `HistoricalIncidentRepository` |
| Entidade JPA | `*Entity` | `HistoricalIncidentEntity` |
| REST Client (MicroProfile) | `*Client` + `@RegisterRestClient` | `TriageServiceClient` |
| Store in-memory | `*Store` | `SimulatedIncidentStore` |
| DTO interno de REST | Inner `record` na classe que o usa | `TriageResource.TriageRequestBody`, `TriageServiceClient.TriageRequestDto` |

## Padrões Java Usados no Projeto

- **Java records** para todos os modelos de domínio e DTOs — não use classes com getters/setters.
- **Java 21 pattern-matching switch** com destructuring — ver `SimulationResource.generate()`.
- **Text blocks** (`"""..."""`) em todas as anotações `@UserMessage` e `@SystemMessage`.
- **`Stream.toList()`** (Java 16+) para conversões de lista — não use `Collectors.toList()`.
- **`AtomicReference`** para estado compartilhado thread-safe (ver `SimulatedIncidentStore`).

## Injeção de Dependência

- `@ApplicationScoped` em todos os beans de serviço, adaptadores e repositório.
- Field injection com `@Inject` — o projeto não usa constructor injection.
- `@Observes StartupEvent` para lógica de inicialização (ver `StartupDataLoader`).
- `@RestClient` combinado com `@Inject` para clientes MicroProfile REST.
- `@RegisterAiService` para que o Quarkus LangChain4j gere o bean CDI em build time.

## Logging

- Use `org.jboss.logging.Logger` — padrão Quarkus presente no projeto.
- Não use `java.util.logging`, SLF4J ou Log4j diretamente.

## Validação

- `@Valid` nos parâmetros de body dos Resources JAX-RS.
- `@NotBlank` (e similares) nos campos dos records de request.
- Retorno HTTP 400 é automático via Hibernate Validator — não capture `ConstraintViolationException` manualmente.

## Prompts de IA

- `@SystemMessage` define o papel do modelo (ex.: "Você é um gestor especialista...").
- `@UserMessage` usa text block com variáveis `{nome}` injetadas automaticamente pelo LangChain4j.
- Tipo de retorno do `@RegisterAiService` é um record — LangChain4j deserializa o JSON para ele.
- Instrua sempre o modelo a responder com JSON válido no `@SystemMessage`.
