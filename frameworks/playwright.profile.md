# Playwright Profile (v1)

- Respeitar estrutura existente do projeto.
- Reutilizar Page Objects e fixtures.
- Usar `@playwright/test`.
- Preferir `getByRole`, `getByTestId`, `getByLabel` e locators estáveis.
- Não usar `waitForTimeout` como sincronização.
- Não usar XPath sem necessidade.
- Usar `expect` do Playwright.
- Não inventar ou duplicar métodos existentes.
- Respeitar `baseURL` e variáveis de ambiente do projeto.
- Não inserir credenciais no código.
- Em TypeScript, usar `.spec.ts`.
- Usar `test.describe` quando fizer sentido.
- Não acessar propriedades privadas de Page Objects.
- Considerar setup/teardown/screenshot/video/trace já configurados.
