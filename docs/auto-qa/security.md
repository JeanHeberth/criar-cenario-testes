# Segurança Auto QA

## Controles
- Paths normalizados com `java.nio.file.Path`.
- Bloqueio de path traversal e diretórios críticos.
- Geração em `.auto-qa/generated/<executionId>/`.
- Backup antes de sobrescrever arquivos.
- DELETE proibido na v1.
- Execução de comandos por whitelist e `ProcessBuilder`.

## Configurações
- `auto-qa.allow-command-execution: false` por padrão.
- `auto-qa.allow-file-application: false` por padrão.

## Dados sensíveis
- Não salvar `.env`, credenciais, tokens ou segredos em logs/relatórios.
