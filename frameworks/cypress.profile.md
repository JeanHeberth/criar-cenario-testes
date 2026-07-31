# Cypress Profile (v1)

- Respeitar estrutura existente do projeto.
- Reutilizar custom commands e fixtures.
- Reutilizar Page Objects quando o projeto já usar o padrão.
- Preferir seletores `data-cy`, `data-testid` e locators estáveis.
- Não usar `cy.wait` fixo como sincronização.
- Usar interceptações quando necessário.
- Não inventar comandos customizados.
- Respeitar `baseUrl`.
- Não inserir credenciais no código.
- Em TypeScript, usar `.cy.ts`.
- Usar `describe`/`it` conforme padrão do projeto.
- Validar respostas e elementos de forma determinística.
