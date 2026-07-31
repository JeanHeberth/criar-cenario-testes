# Arquitetura Auto QA

## Camadas
- `controller`: endpoints REST.
- `business/autoqa/workflow`: orquestração e contexto.
- `business/autoqa/agent`: agentes especializados.
- `business/autoqa/framework`: adapters por framework.
- `business/autoqa/service`: validação, scanner, storage, aplicação, execução.
- `infrastructure/entity|repository`: persistência no MongoDB.

## Princípios
- Estado centralizado no `AutoQaContext`.
- Agentes sem acesso direto a controller/repository.
- Segurança por padrão (execução/aplicação desabilitadas por configuração).
