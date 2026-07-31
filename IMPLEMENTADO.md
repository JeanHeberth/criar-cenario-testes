# AUTO QA BMAD — Implementado

## Visão geral
Foi implementada a arquitetura BMAD no fluxo AUTO QA com separação de responsabilidades, endpoints dedicados, persistência enriquecida, UI Angular integrada e documentação operacional.

## Backend (Spring Boot)
- Orquestração do fluxo AUTO QA com etapas especializadas e contexto compartilhado.
- Endpoints adicionados/ajustados:
  - `POST /auto-qa/executions/{executionId}/generate`
  - `POST /auto-qa/executions/{executionId}/apply`
  - `POST /auto-qa/executions/{executionId}/execute`
  - `POST /auto-qa/executions/{executionId}/discard`
  - `GET /auto-qa/executions/{executionId}/generated-files`
  - `GET /auto-qa/executions/{executionId}/generated-files/content`
- Compatibilidade mantida com rota legada de aplicação de arquivos.
- Correção de mapeamento de arquivos para Spring Boot 3.4.x (`files/**`).
- Serviços especializados adicionados:
  - `CommandPolicyService`
  - `TestExecutionService`
  - `GeneratedFileApplicationService`
  - `ProjectCatalogService`
- Persistência Mongo ampliada com campos de descoberta, análise, revisão, execução e falha.
- `AutoQaResponse` enriquecido com dados completos para exibição no frontend.

## Segurança e aplicação de arquivos
- Geração isolada em `.auto-qa/generated/<executionId>`.
- Bloqueio de path traversal.
- Operação `DELETE` não permitida no fluxo de aplicação.
- Regras de `UPDATE` com `allowFileUpdate`.
- Validação de hash do gerado e detecção de alteração externa quando aplicável.

## Manifesto de geração
- `manifest.json` com metadados de execução:
  - projeto, framework, linguagem, cenário, revisão, status
  - lista de arquivos com operação e hashes

## Frontend (Angular)
- Tela `autoqa-artifacts` implementada e integrada ao menu/rotas.
- Formulário completo com validação e execução das etapas do fluxo.
- Integração HTTP com endpoints reais de validate/analyze/generate/apply/execute/discard.
- Exibição detalhada de:
  - descoberta de projeto
  - análise técnica
  - plano técnico
  - arquivos gerados e preview
  - revisão de código
  - execução de testes (stdout/stderr)
  - análise de falha
- Build ajustado para tipagem estrita e null-safety em template.

## Estrutura BMAD e documentação
- Arquivos de agentes em `agents/auto-qa/*.agent.md`.
- Workflow em `workflows/auto_qa.workflow.md`.
- Perfis em `frameworks/playwright.profile.md` e `frameworks/cypress.profile.md`.
- Documentação em `docs/auto-qa/` (arquitetura, workflow, segurança, operação).
- `AgentLoaderService` atualizado para suportar subpastas (`agents/**/*.agent.md`).

## Estado atual
- Backend: build concluído com sucesso.
- Frontend: build concluído com sucesso.
- Pendências remanescentes estão concentradas nas tarefas abertas de Fase 9/10 do backlog técnico.
