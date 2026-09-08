# Cenário enriquecido — Autenticação via API (login)

**Data:** 2026-08-31
**Execução original:** `508453c4-6942-4e7a-98ac-2bc76c9ba74c` (10/22 passaram, 12 falharam)
**Motivo:** o cenário original não trazia o texto literal das mensagens de erro
em dois pontos; o agente as inventou (regra `MENSAGEM_NAO_ESPECIFICADA`
bloqueou o apply corretamente — ver
`docs/relatorios/2026-08-25-mensagem-nao-especificada-e-log-de-blocked.md`).

## Apuração da limpeza pedida ("limpe o banco antes")

Escopo combinado: apagar só o documento desta execução em
`autoqa_executions` (banco `geradorcenarios`, cluster apontado por
`MONGO_URI_NUVEM`).

**Resultado: não havia nada para apagar.** A coleção tem 8 documentos, todos
de 2026-08-21/22 — nenhum de 2026-08-25. O documento de
`508453c4-6942-4e7a-98ac-2bc76c9ba74c` nunca chegou a ficar persistido nesse
banco (ou já tinha sido removido antes). Nenhuma ação destrutiva foi
necessária.

**Achado colateral valioso:** os 8 documentos existentes contêm o
`scenarioSummary` **original e real** deste mesmo recurso de login (mesmos 12
casos, mesmos IDs do Zephyr `SCRUM-T415`..`SCRUM-T426`, texto idêntico nos 8 —
ou seja, foi reenviado várias vezes sem alteração). Esse texto confirma o
diagnóstico: os itens "Cenário 6" e "Cenário 8" do contrato original dizem
literalmente **"registrar o comportamento observado para validação
posterior"**, sem nunca definir a mensagem exata — daí o agente ter
preenchido com algo plausível, porém errado.

O cenário abaixo é esse texto original, **preservado quase integralmente**,
com apenas os itens 6 e 8 completados com as mensagens reais (fonte:
`/Users/jeanheberth/Development/api/criandoAPI`, arquivos `LoginRequest.java`
e `GlobalExceptionHandler.java`).

---

## Cenário a enviar (campo `scenario` do POST /api/auto-qa/executions)

```
Título: Autenticação de usuário via API (login)

Regra de negócio:
O usuário deve conseguir autenticar informando e-mail e senha cadastrados. Credencial incorreta não autentica e não retorna token. Campos obrigatórios vazios são rejeitados antes da tentativa de autenticação, com detalhamento por campo.

CONTRATO DA REQUISIÇÃO
- Método e caminho: POST auth/login (relativo à baseURL do projeto)
- Cabeçalho: Content-Type: application/json
- Corpo, com estes nomes de campo exatos: email (string), senha (string)

RESPOSTA 200 (credenciais válidas)
{ "token": "<JWT: três partes separadas por ponto, alfabeto base64url [A-Za-z0-9_-]>", "tipo": "Bearer", "usuario": { "id": 2 (número inteiro), "nome": "Usuário Automação", "email": "teste@gmail.com" } }

RESPOSTA 401 (senha incorreta)
{ "status": 401, "erro": "401 UNAUTHORIZED", "mensagem": "Credenciais inválidas", "path": "/criandoAPI/v1/auth/login", "timestamp": "ISO-8601" }
Não retorna os campos "token" nem "usuario".

RESPOSTA 400 (email e senha vazios)
{ "status": 400, "erro": "Erro de Validação", "mensagem": "Um ou mais campos estão inválidos", "path": "/criandoAPI/v1/auth/login", "timestamp": "ISO-8601", "campos": [ { "campo": "senha", "mensagem": "Senha é obrigatória" }, { "campo": "email", "mensagem": "E-mail é obrigatório" } ] }
A ordem dos itens de "campos" não é garantida — não depender do índice.

RESPOSTA 400 (JSON estruturalmente malformado, ex.: chave não fechada)
{ "status": 400, "erro": "Requisicao Invalida", "mensagem": "O corpo da requisicao esta mal formado ou contem valor invalido.", "path": "/criandoAPI/v1/auth/login", "timestamp": "ISO-8601" }
Mensagem EXATA (sem acentuação, é assim que a API realmente devolve) — não usar "JSON parse error" nem variações acentuadas.

VARIÁVEIS DE AMBIENTE DO PROJETO (usar exatamente estes nomes)
- API_BASE_URL (base da API, já configurada com barra no final)
- AUTH_USERNAME (e-mail válido para o teste positivo)
- AUTH_PASSWORD (senha válida correspondente)
- AUTH_INVALID_PASSWORD (senha propositalmente incorreta, para o cenário 401)

Cenários de teste a automatizar (12):

--- Cenário 1 ---
Nome: Login bem-sucedido com e-mail e senha válidos
Objetivo: Validar que o usuário consegue autenticar com e-mail e senha válidos e recebe resposta conforme contrato.
Pré-condições: Usuário com e-mail e senha válidos cadastrados; variáveis de ambiente configuradas.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E o usuário possui e-mail e senha válidos cadastrados
E a requisição possui o cabeçalho Content-Type igual a application/json
Quando uma requisição POST é enviada para o endpoint com corpo JSON contendo os campos "email" e "senha" preenchidos corretamente
Resultado esperado: Então a resposta deve conter status HTTP 200
E o corpo da resposta deve conter um campo "token" com três partes separadas por ponto e caracteres válidos base64url
E o campo "tipo" deve ser igual a "Bearer"
E o campo "usuario" deve conter "id" (inteiro), "nome" (string) e "email" igual ao enviado Usuário autenticado, token JWT válido retornado, estrutura da resposta exatamente conforme contrato.
Caso de teste no Zephyr: SCRUM-T415

--- Cenário 2 ---
Nome: Autenticação falha com senha inválida ou e-mail inexistente
Objetivo: Garantir que o login falha para senha incorreta ou e-mail não cadastrado, retornando resposta 401 padronizada, sem expor existência de credenciais.
Pré-condições: - Cenário 1: Usuário com e-mail válido cadastrado; senha inválida definida em AUTH_INVALID_PASSWORD.
- Cenário 2: E-mail informado não está cadastrado.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição possui o cabeçalho Content-Type igual a application/json
E um dos casos: | email cadastrado e senha inválida | email não cadastrado e senha qualquer |
Quando uma requisição POST é enviada para o endpoint com o corpo JSON contendo "email" e "senha"
Resultado esperado: Então a resposta deve conter status HTTP 401
E o corpo da resposta deve conter "status": 401, "erro": "401 UNAUTHORIZED", "mensagem": "Credenciais inválidas", "path" correspondente ao endpoint e "timestamp" em ISO-8601
E não devem existir os campos "token" nem "usuario" no corpo da resposta Login rejeitado, sem exposição de informações sensíveis, resposta 401 conforme contrato, sem token/usuário.
Caso de teste no Zephyr: SCRUM-T416

--- Cenário 3 ---
Nome: Falha por campos obrigatórios ausentes ou vazios
Objetivo: Garantir que ausência ou vazio em campos obrigatórios resulta em resposta 400 com detalhamento por campo.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição possui o cabeçalho Content-Type igual a application/json
Quando uma requisição POST é enviada para o endpoint com um dos casos abaixo: | ambos "email" e "senha" ausentes | apenas "email" ausente | apenas "senha" ausente | ambos presentes mas vazios ("") | corpo JSON vazio ({}) |
Resultado esperado: Então a resposta deve conter status HTTP 400
E o corpo da resposta deve conter "status": 400, "erro": "Erro de Validação", "mensagem": "Um ou mais campos estão inválidos", "path" e "timestamp" em ISO-8601
E o array "campos" deve detalhar individualmente os campos faltantes ou vazios, cada um com o campo "mensagem" correspondente à obrigatoriedade Erro 400 com array "campos" detalhando os campos obrigatórios ausentes ou vazios, ordem não relevante.
Caso de teste no Zephyr: SCRUM-T417

--- Cenário 4 ---
Nome: Falha por envio da requisição sem o cabeçalho Content-Type: application/json
Objetivo: Garantir que o endpoint rejeita requisições sem o cabeçalho obrigatório e retorna erro apropriado.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição é enviada sem o cabeçalho Content-Type ou com valor diferente de application/json
Quando uma requisição POST é enviada para o endpoint com corpo JSON válido
Resultado esperado: Então a resposta deve conter erro informando rejeição por cabeçalho inválido ou ausente
E o status deve ser apropriado conforme implementação (ex: 415, 400) Rejeição da requisição sem Content-Type: application/json, erro apropriado retornado.
Caso de teste no Zephyr: SCRUM-T418

--- Cenário 5 ---
Nome: Falha por uso de método HTTP diferente de POST
Objetivo: Garantir que o endpoint não aceita métodos diferentes de POST.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
Quando uma requisição GET (ou PUT, DELETE, PATCH, OPTIONS) é enviada para o endpoint
Resultado esperado: Então a resposta deve ser uma rejeição apropriada (ex: 405 Method Not Allowed ou 404 Not Found)
E não deve ser possível autenticar usando outro método que não POST Endpoint rejeita métodos HTTP diferentes de POST, sem autenticação.
Caso de teste no Zephyr: SCRUM-T419

--- Cenário 6 ---
Nome: Validação de formatos e tipos inválidos em campos obrigatórios
Objetivo: Verificar comportamento do endpoint ao receber valores inválidos nos campos "email" e "senha".
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição possui o cabeçalho Content-Type igual a application/json
Quando uma requisição POST é enviada para o endpoint com um dos casos abaixo: | "email" com formato inválido | "email": null | "senha": null | "senha" como número | "email" ou "senha" apenas com espaços em branco |
Resultado esperado: Então a resposta deve conter status HTTP 400 em todos os casos, com "campos" detalhando cada um exatamente assim (mensagens EXATAS, não usar sinônimos):
- "email" com formato inválido (contém "@" mas não segue o padrão de e-mail, ex.: "invalido.com", "invalido@"): { "campo": "email", "mensagem": "E-mail deve ter formato válido" } — NÃO é "E-mail deve ser um endereço de e-mail válido", essa variante não existe na API.
- "email": null: { "campo": "email", "mensagem": "E-mail é obrigatório" }
- "senha": null: { "campo": "senha", "mensagem": "Senha é obrigatória" }
- "senha" como número (ex.: 12345, sem aspas no JSON): a API desserializa o número como texto antes de validar, então @NotBlank passa — NÃO espere 400 "Senha é obrigatória" aqui. O comportamento real é a tentativa de login prosseguir com essa senha convertida para string, terminando em 401 "Credenciais inválidas" (a menos que coincida com a senha real, aí 200). Gerar o teste cobrindo esse comportamento, não a suposição de 400.
- "email" ou "senha" apenas com espaços em branco ("   "): como o valor não é nulo, tanto @NotBlank quanto @Email (quando o campo é email) são avaliados juntos — para email: os DOIS itens simultaneamente, { "campo": "email", "mensagem": "E-mail é obrigatório" } e { "campo": "email", "mensagem": "E-mail deve ter formato válido" }; para senha: só { "campo": "senha", "mensagem": "Senha é obrigatória" }.
Caso de teste no Zephyr: SCRUM-T420

--- Cenário 7 ---
Nome: Falha por envio de campos extras no corpo da requisição
Objetivo: Verificar como o endpoint reage quando campos não especificados no contrato são enviados.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição possui o cabeçalho Content-Type igual a application/json
Quando uma requisição POST é enviada para o endpoint com campos extras além de "email" e "senha" no corpo JSON
Resultado esperado: Então registrar o comportamento observado quanto à aceitação, rejeição ou ignorância dos campos extras e estrutura da resposta Validar se campos extras são ignorados ou provocam erro, e se resposta segue o contrato.
Caso de teste no Zephyr: SCRUM-T421

--- Cenário 8 ---
Nome: Robustez do endpoint frente a corpo malformado (JSON inválido)
Objetivo: Verificar o comportamento do endpoint ao receber corpo da requisição com JSON estruturalmente malformado (ex.: chave não fechada, vírgula sobrando).
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E a requisição possui o cabeçalho Content-Type igual a application/json
Quando uma requisição POST é enviada para o endpoint com corpo JSON malformado
Resultado esperado: Então a resposta deve conter status HTTP 400
E "erro" deve ser exatamente "Requisicao Invalida" (sem acentuação)
E "mensagem" deve ser exatamente "O corpo da requisicao esta mal formado ou contem valor invalido." (sem acentuação) — NÃO usar "JSON parse error" nem variações acentuadas, essa substring não aparece na resposta real.
Caso de teste no Zephyr: SCRUM-T422

--- Cenário 9 ---
Nome: Validação da estrutura e nomes dos campos nas respostas (case-sensitive)
Objetivo: Garantir que os nomes dos campos na resposta seguem exatamente o especificado, incluindo caixa, sem variações.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E uma requisição válida (ou inválida) é enviada conforme o cenário
Quando a resposta é recebida
Resultado esperado: Então verificar que todos os nomes dos campos da resposta são exatamente os definidos no contrato, incluindo caixa (case-sensitive), sem campos extras ou variações Estrutura e nomes dos campos da resposta seguem rigorosamente o contrato, sem variações ou campos indevidos.
Caso de teste no Zephyr: SCRUM-T423

--- Cenário 10 ---
Nome: Validação do formato do token JWT retornado
Objetivo: Garantir que o token JWT retornado possui três partes separadas por ponto e usa apenas caracteres base64url válidos.
Pré-condições: Usuário com e-mail e senha válidos cadastrados.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E o usuário possui credenciais válidas
Quando uma requisição POST é enviada para o endpoint com os campos obrigatórios preenchidos corretamente
Resultado esperado: Então a resposta deve conter um campo "token" formado por três partes separadas por ponto
E cada parte deve conter apenas caracteres válidos em base64url [A-Za-z0-9_-] Token JWT retornado segue o padrão de três partes e caracteres base64url.
Caso de teste no Zephyr: SCRUM-T424

--- Cenário 11 ---
Nome: Validação do campo "path" nas respostas de erro
Objetivo: Verificar se o campo "path" nas respostas de erro reflete exatamente o endpoint chamado, conforme contrato.
Pré-condições: Endpoint disponível.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
E uma requisição inválida é enviada para provocar erro 400 ou 401
Quando a resposta de erro é recebida
Resultado esperado: Então verificar se o campo "path" corresponde exatamente ao endpoint chamado, conforme esperado pelo contrato O campo "path" nas respostas de erro reflete corretamente o endpoint chamado.
Caso de teste no Zephyr: SCRUM-T425

--- Cenário 12 ---
Nome: Validação do tempo de resposta do endpoint em fluxos principais
Objetivo: Garantir que as respostas do endpoint ocorrem em tempo adequado (<2 segundos).
Pré-condições: Endpoint disponível; ambiente com latência razoável.
Passos:
Dado que o endpoint ${API_BASE_URL}auth/login está disponível
Quando uma requisição POST válida é enviada para o endpoint
Resultado esperado: Então a resposta deve ser recebida em tempo inferior a 2 segundos Tempo de resposta do endpoint dentro do limite definido para APIs síncronas.
Caso de teste no Zephyr: SCRUM-T426
```

## Execução do pipeline

`projectPath`: `/Users/jeanheberth/Development/AutomcaoPW/testando-AUTO-QA`
(projeto Playwright/TypeScript real onde os testes anteriores foram gerados —
localizado pelo `playwright.config.ts` e pela ausência de qualquer arquivo de
contrato nos dois diretórios originalmente sugeridos).

`automationType`: `API` · `automationFramework`: `PLAYWRIGHT` (informados
explicitamente para não repetir a etapa de discovery).
