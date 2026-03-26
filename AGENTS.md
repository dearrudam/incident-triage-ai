# incident-triage-ai

Sistema de triagem automática de incidentes de TI usando embeddings vetoriais, busca semântica por similaridade e LLM, implementado com Quarkus + LangChain4j.

## Comportamento do Agente

**Fazer:**
- Leia o arquivo antes de propor qualquer alteração e explique o motivo da mudança.
- Respeite a separação de camadas (domínio puro → portas → adaptadores). Consulte [docs/architecture.md](docs/architecture.md).
- Siga as convenções de nomeação e padrões de código já estabelecidos. Consulte [docs/conventions.md](docs/conventions.md).
- Novas funcionalidades devem seguir o workflow OpenSpec: `openspec propose` → `openspec apply` → `openspec archive`.

**Não fazer:**
- Não adicione imports de frameworks (`jakarta.*`, `quarkus.*`, `langchain4j.*`) em `domain/model/` ou `domain/port/`.
- Não troque o modelo de embedding sem considerar que a dimensão vetorial (`vector(768)`) está fixada no schema.
- Não documente caminhos internos de pastas ou detalhes de implementação neste arquivo — delegue para os arquivos especializados abaixo.

## Arquivos Especializados

| Arquivo | Quando consultar |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Módulos, camadas, fluxo de triagem, restrições arquiteturais |
| [docs/conventions.md](docs/conventions.md) | Nomeação, padrões Java, DI, logging, validação |
| [docs/development.md](docs/development.md) | Build, execução, infraestrutura, variáveis de ambiente, testes |

## Limites do Agente

- Alterações no schema do banco impactam dados em runtime — o DDL é `drop-and-create` no perfil MVP.
- Não existem testes automatizados ainda; a validação atual é smoke test manual.
- Não há autenticação, retry ou observabilidade — exclusões intencionais do escopo MVP.
- Changes arquivados ficam em `openspec/changes/archive/<data>-<slug>/` — não edite arquivos arquivados.
