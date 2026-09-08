# SonarQube Cloud + Quality Gate

Análise estática e gate de cobertura que **reprova o Pull Request** quando o
código novo não atende aos critérios de qualidade.

## Como funciona neste projeto

- **JaCoCo** (`build.gradle`) gera `build/reports/jacoco/test/jacocoTestReport.xml`.
- A task `sonar` do Gradle envia código + relatório de cobertura para o
  SonarQube Cloud.
- O workflow **Backend CI** (`.github/workflows/gradle.yml`, job
  `SonarQube Cloud Analysis`) roda a análise em todo PR para `develop` e `main`.
- O SonarQube Cloud publica o check **"SonarQube Code Analysis"** no PR com o
  resultado do Quality Gate.

Verificação local de cobertura (independente do Sonar):

```bash
./gradlew check          # roda test + jacocoTestCoverageVerification
```

Gate local em `build.gradle`: instruções ≥ 75%, branches ≥ 60% (bundle inteiro).

## Setup inicial (uma vez)

1. **Criar a organização/projeto**
   - Acesse <https://sonarcloud.io> e entre com o GitHub.
   - *Import an organization from GitHub* → selecione a conta `JeanHeberth`.
   - *Analyze new project* → `criar-cenario-testes`.
   - Em *Administration → Analysis Method*, escolha **CI-based** (Gradle) e
     desative o *Automatic Analysis* (senão conflita com a task do Gradle).

2. **Conferir as chaves**
   - Confirme `sonar.organization` e `sonar.projectKey` em `build.gradle`
     contra o que aparece no painel. A convenção do GitHub é
     `organization = jeanheberth` e `projectKey = JeanHeberth_criar-cenario-testes`.

3. **Gerar o token**
   - *My Account → Security → Generate Token* (tipo *Project Analysis Token*).
   - No GitHub: *Settings → Secrets and variables → Actions → New repository
     secret* → nome `SONAR_TOKEN`, valor = token gerado.

4. **Configurar o Quality Gate ("Clean as You Code")**
   - Em *Quality Gates*, use o `Sonar way` ou crie um customizado com,
     no **código novo**:
     - Coverage ≥ 80%
     - 0 novos bugs / vulnerabilidades / security hotspots pendentes
     - Duplicated lines ≤ 3%
   - Associe o Quality Gate ao projeto.

5. **Tornar o check obrigatório**
   - GitHub: *Settings → Branches → Branch protection rules* para `develop` e
     `main` → *Require status checks to pass* → marque
     **`SonarQube Code Analysis`**.
   - A partir daí, PR com Quality Gate vermelho não faz merge.

## Rodar a análise localmente (opcional)

```bash
export SONAR_TOKEN=xxxxxxxx
./gradlew test jacocoTestReport sonar
```

## Ajustes comuns

- **Excluir arquivos da cobertura**: `sonar.coverage.exclusions` em
  `build.gradle` (hoje: `*Application`, `config/**`, `dto/**`, `*Config`,
  `*Configuration`).
- **Excluir da análise inteira**: adicione `sonar.exclusions`.
- **Mudar o alvo local**: `minimum` nas `violationRules` de
  `jacocoTestCoverageVerification`.
