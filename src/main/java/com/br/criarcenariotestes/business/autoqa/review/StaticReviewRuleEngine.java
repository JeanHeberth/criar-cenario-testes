package com.br.criarcenariotestes.business.autoqa.review;

import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationLanguage;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFile;
import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;
import com.br.criarcenariotestes.business.autoqa.model.generation.GenerationResult;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanComponentType;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.TechnicalPlanResult;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewCategory;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewIssue;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewRule;
import com.br.criarcenariotestes.business.autoqa.model.review.ReviewSeverity;
import com.br.criarcenariotestes.business.autoqa.security.PadroesDeConteudoProibido;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class StaticReviewRuleEngine {

    private static boolean temSegredoLiteral(String content) {
        return PadroesDeConteudoProibido.contemSegredoLiteral(content);
    }

    /** .env.example, .env.sample, .env.template — arquivos cujo conteúdo É exemplo. */
    private static boolean ehTemplateDeAmbiente(String path) {
        if (path == null) return false;
        String nome = path.substring(path.lastIndexOf('/') + 1).toLowerCase(java.util.Locale.ROOT);
        return nome.startsWith(".env.") || nome.equals("env.example") || nome.endsWith(".env.example");
    }

    static final int FILE_TOO_LARGE_THRESHOLD = 8_000;
    static final int DUPLICATED_LINE_THRESHOLD = 3;
    private static final int MIN_DUPLICATE_LINE_LENGTH = 12;

    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\./");
    private static final Pattern ABSOLUTE_UNIX = Pattern.compile("^/");
    private static final Pattern ABSOLUTE_WINDOWS = Pattern.compile("(?i)^[A-Za-z]:\\\\");
    private static final Pattern ABSOLUTE_UNC = Pattern.compile("^\\\\\\\\");
    private static final Pattern FILE_URI = Pattern.compile("(?i)^file://");
    private static final Pattern MARKDOWN_FENCE = Pattern.compile("```");
    private static final Pattern CREDENTIAL_BEARER = Pattern.compile("(?i)bearer\\s+[a-z0-9._-]{8,}");
    private static final Pattern URL_WITH_CREDENTIALS = Pattern.compile(
            "(?i)[a-z][a-z0-9+.-]*://[^\\s/?#:@]+:[^\\s/?#@]+@");
    private static final Pattern HARDCODED_URL = Pattern.compile("(?i)https?://[\\w.-]+");
    private static final Pattern THREAD_SLEEP = Pattern.compile("Thread\\.sleep\\s*\\(");
    private static final Pattern CYPRESS_FIXED_WAIT = Pattern.compile("cy\\.wait\\s*\\(\\s*\\d+");
    private static final Pattern PLAYWRIGHT_FIXED_WAIT = Pattern.compile("waitForTimeout\\s*\\(");
    private static final Pattern ROBOT_SLEEP = Pattern.compile("(?m)^\\s*Sleep\\s+\\S+");
    private static final Pattern WILDCARD_IMPORT = Pattern.compile("(?m)^\\s*import\\s+[\\w.]+\\.\\*;");
    private static final Pattern GENERIC_EXCEPTION = Pattern.compile("catch\\s*\\(\\s*(Exception|Throwable|Error)\\s+\\w+\\s*\\)");
    private static final Pattern EMPTY_BLOCK = Pattern.compile("\\{\\s*\\}");
    private static final Pattern FRAGILE_SELECTOR = Pattern.compile(":nth-child|(>\\s*[\\w.#\\[\\]]+\\s*){3,}");
    /**
     * Três famílias de assertion escapavam e faziam teste Java legítimo virar
     * MISSING_ASSERTION (HIGH), que trava o apply:
     *
     * - "Assertions\." só pega JUnit 5. TestNG e JUnit 4 usam "Assert." no
     *   singular — e TestNG é o que este ecossistema usa. (Cuidado: escrever
     *   "Assertions?\." NÃO resolve, porque o "?" recai sobre o "s" de
     *   "Assertion", não sobre "ions" — é preciso agrupar.)
     * - assertTrue/assertEquals/assertNotNull vindos de import estático não
     *   têm prefixo de classe nenhum, e "assert " com espaço não os alcança.
     * - "\.should\(" exige o parêntese colado, então .shouldBe(visible) e
     *   .shouldHave(text) do Selenide não casavam; são justamente a forma
     *   normal de assertar em Selenide, onde não existe assert avulso.
     */
    private static final Pattern ASSERTION_EVIDENCE = Pattern.compile(
            "expect\\(|assertThat|Assert(?:ions)?\\.|assert[A-Za-z]*\\s*\\(|\\.should[A-Za-z]*\\("
                    + "|assert |Should Be|Should Contain");

    private record FrameworkRule(Set<String> extensions, List<String> requiredAny, List<String> forbidden) {}

    private static final Map<AutomationFramework, FrameworkRule> FRAMEWORK_RULES = new EnumMap<>(AutomationFramework.class);

    static {
        FRAMEWORK_RULES.put(AutomationFramework.PLAYWRIGHT, new FrameworkRule(
                Set.of("ts", "js"),
                List.of("@playwright/test", "Page", "Locator", "expect(", "test("),
                List.of("cy.", "Cypress.Commands")
        ));
        FRAMEWORK_RULES.put(AutomationFramework.CYPRESS, new FrameworkRule(
                Set.of("ts", "js", "json"),
                List.of("describe(", "it(", "cy."),
                List.of("@playwright/test")
        ));
        FRAMEWORK_RULES.put(AutomationFramework.SELENIDE, new FrameworkRule(
                Set.of("java"),
                List.of("Selenide", "SelenideElement", "com.codeborne.selenide"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.SELENIUM, new FrameworkRule(
                Set.of("java"),
                List.of("WebDriver", "WebElement", "org.openqa.selenium"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.REST_ASSURED, new FrameworkRule(
                Set.of("java"),
                List.of("RestAssured", "RequestSpecification", "Response", "io.restassured"),
                List.of()
        ));
        FRAMEWORK_RULES.put(AutomationFramework.ROBOT_FRAMEWORK, new FrameworkRule(
                Set.of("robot", "resource"),
                List.of("*** Test Cases ***", "*** Keywords ***", "Resource", "Library"),
                List.of()
        ));
    }

    public List<ReviewIssue> review(AutomationFramework framework,
                                     AutomationLanguage language,
                                     TechnicalPlanResult plan,
                                     GenerationResult generation,
                                     List<GeneratedArtifactReader.ReadArtifact> artifacts) {
        return review(framework, language, plan, generation, artifacts, null);
    }

    public List<ReviewIssue> review(AutomationFramework framework,
                                     AutomationLanguage language,
                                     TechnicalPlanResult plan,
                                     GenerationResult generation,
                                     List<GeneratedArtifactReader.ReadArtifact> artifacts,
                                     VocabularioDoContrato contrato) {
        return review(framework, language, plan, generation, artifacts, contrato, Set.of());
    }

    public List<ReviewIssue> review(AutomationFramework framework,
                                     AutomationLanguage language,
                                     TechnicalPlanResult plan,
                                     GenerationResult generation,
                                     List<GeneratedArtifactReader.ReadArtifact> artifacts,
                                     VocabularioDoContrato contrato,
                                     Set<String> statusDoCenario) {
        List<ReviewIssue> issues = new ArrayList<>();
        issues.addAll(reviewPlanAdherence(plan, generation));
        for (GeneratedArtifactReader.ReadArtifact artifact : artifacts) {
            issues.addAll(reviewFile(framework, language, artifact));
            issues.addAll(reviewFidelidadeAoContrato(contrato, artifact));
            issues.addAll(reviewSkipPorAmbiente(artifact));
            issues.addAll(reviewStatusNaoDefinido(statusDoCenario, artifact));
        }
        issues.addAll(reviewFrameworkEvidenceNoConjunto(framework, artifacts));
        return List.copyOf(issues);
    }

    /**
     * Se NENHUM arquivo gerado carrega marca do framework, aí sim há
     * incompatibilidade real — provavelmente foi gerado para o framework errado.
     * Basta um arquivo com a evidência para o conjunto estar coerente.
     */
    private List<ReviewIssue> reviewFrameworkEvidenceNoConjunto(AutomationFramework framework,
                                                                List<GeneratedArtifactReader.ReadArtifact> artifacts) {
        FrameworkRule rule = FRAMEWORK_RULES.get(framework);
        if (rule == null || artifacts == null || artifacts.isEmpty()) {
            return List.of();
        }
        boolean algumComEvidencia = artifacts.stream()
                .filter(a -> a != null && a.content() != null)
                .anyMatch(a -> rule.requiredAny().stream().anyMatch(a.content()::contains));
        if (algumComEvidencia) {
            return List.of();
        }
        return List.of(issue(ReviewRule.FRAMEWORK_MISMATCH, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.HIGH,
                null, null, "Nenhum arquivo gerado apresenta evidência do framework " + framework, null,
                "Confirmar se a geração usou o framework do projeto"));
    }

    private List<ReviewIssue> reviewPlanAdherence(TechnicalPlanResult plan, GenerationResult generation) {
        List<ReviewIssue> issues = new ArrayList<>();

        Map<String, PlannedFileAction> plannedCreateOrUpdate = new LinkedHashMap<>();
        for (PlannedFileAction action : plan.fileActions()) {
            if (action == null) continue;
            if (action.operation() == FileOperation.CREATE || action.operation() == FileOperation.UPDATE) {
                plannedCreateOrUpdate.put(action.relativePath(), action);
            }
        }

        Map<String, GeneratedFile> generatedCreateOrUpdate = new LinkedHashMap<>();
        for (GeneratedFile file : generation.files()) {
            if (file == null) continue;
            if (file.operation() == GeneratedFileOperation.CREATE || file.operation() == GeneratedFileOperation.UPDATE) {
                generatedCreateOrUpdate.put(file.relativePath(), file);
            }
        }

        for (String plannedPath : plannedCreateOrUpdate.keySet()) {
            if (!generatedCreateOrUpdate.containsKey(plannedPath)) {
                issues.add(issue(ReviewRule.PLAN_FILE_MISSING, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        plannedPath, null, "Arquivo planejado não foi gerado", null,
                        "Gerar o arquivo planejado ou justificar formalmente a omissão"));
            }
        }

        for (Map.Entry<String, GeneratedFile> entry : generatedCreateOrUpdate.entrySet()) {
            PlannedFileAction planned = plannedCreateOrUpdate.get(entry.getKey());
            if (planned == null) {
                issues.add(issue(ReviewRule.PLAN_FILE_EXTRA, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        entry.getKey(), null, "Arquivo gerado não estava no plano técnico", null,
                        "Remover o arquivo ou atualizar o plano técnico"));
                continue;
            }
            if (!planned.operation().name().equals(entry.getValue().operation().name())) {
                issues.add(issue(ReviewRule.PLAN_OPERATION_MISMATCH, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.HIGH,
                        entry.getKey(), null, "Operação divergente do plano técnico",
                        "plano=" + planned.operation() + " gerado=" + entry.getValue().operation(),
                        "Ajustar a operação para corresponder ao plano"));
            }
            if (planned.componentType() != entry.getValue().componentType()) {
                issues.add(issue(ReviewRule.PLAN_COMPONENT_MISMATCH, ReviewCategory.PLAN_ADHERENCE, ReviewSeverity.MEDIUM,
                        entry.getKey(), null, "Tipo de componente divergente do plano técnico",
                        "plano=" + planned.componentType() + " gerado=" + entry.getValue().componentType(),
                        "Ajustar o componentType para corresponder ao plano"));
            }
        }

        return issues;
    }

    private List<ReviewIssue> reviewFile(AutomationFramework framework, AutomationLanguage language,
                                          GeneratedArtifactReader.ReadArtifact artifact) {
        List<ReviewIssue> issues = new ArrayList<>();
        String path = artifact.relativePath();
        String content = artifact.content();
        String extension = extensionOf(path);
        // Arquivo de configuração (.env.example, config do runner) não é código do
        // framework nem da linguagem: não tem a extensão de nenhum dos dois. As
        // regras de compatibilidade não se aplicam a ele — cobrá-las gerava achado
        // HIGH num auxiliar legítimo e travava o apply.
        boolean configuracao = artifact.componentType() == PlanComponentType.CONFIGURATION
                || ehTemplateDeAmbiente(path);

        if (PATH_TRAVERSAL.matcher(path).find()) {
            issues.add(critical(ReviewRule.PATH_TRAVERSAL, ReviewCategory.SECURITY, path, "Path traversal detectado no caminho gerado"));
        }
        if (ABSOLUTE_UNIX.matcher(path).find() || ABSOLUTE_WINDOWS.matcher(path).find()
                || ABSOLUTE_UNC.matcher(path).find() || FILE_URI.matcher(path).find()) {
            issues.add(critical(ReviewRule.ABSOLUTE_PATH, ReviewCategory.SECURITY, path, "Caminho absoluto detectado"));
        }

        FrameworkRule rule = FRAMEWORK_RULES.get(framework);
        // Configuração (.env.example, config do runner) não segue as regras do
        // framework: não tem a extensão dele nem seus imports. Cobrá-las gerava
        // achado em arquivo auxiliar legítimo e travava o apply.
        if (rule != null && !configuracao) {
            if (!rule.extensions().contains(extension)) {
                issues.add(issue(ReviewRule.INVALID_EXTENSION, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.MEDIUM,
                        path, null, "Extensão incompatível com o framework " + framework, extension,
                        "Usar uma das extensões esperadas: " + rule.extensions()));
            } else if (!"json".equals(extension)) {
                // A evidência do framework é exigida no CONJUNTO (ver review()),
                // não arquivo a arquivo: uma geração bem estruturada traz model,
                // client de API e helper que legitimamente não referenciam o
                // framework de teste — um "usuario.ts" é só uma interface. Exigir
                // a marca em cada um marcava HIGH nesses arquivos e bloqueava o
                // apply de um resultado correto.
                for (String forbidden : rule.forbidden()) {
                    if (content.contains(forbidden)) {
                        issues.add(issue(ReviewRule.FRAMEWORK_MISMATCH, ReviewCategory.FRAMEWORK_COMPATIBILITY, ReviewSeverity.HIGH,
                                path, null, "Evidência de framework incompatível detectada", forbidden,
                                "Remover código de framework incompatível"));
                    }
                }
            }
        }

        if (!configuracao && !isExtensionCompatibleWithLanguage(language, extension)) {
            issues.add(issue(ReviewRule.LANGUAGE_MISMATCH, ReviewCategory.LANGUAGE_COMPATIBILITY, ReviewSeverity.HIGH,
                    path, null, "Extensão incompatível com a linguagem " + language, extension,
                    "Ajustar a extensão conforme a linguagem do projeto"));
        }

        if (MARKDOWN_FENCE.matcher(content).find()) {
            issues.add(issue(ReviewRule.MARKDOWN_FENCE, ReviewCategory.CODE_QUALITY, ReviewSeverity.HIGH,
                    path, null, "Bloco Markdown encontrado no conteúdo gerado", "```", "Remover marcação Markdown do arquivo"));
        }

        if (temSegredoLiteral(content)) {
            // Em template de ambiente (.env.example e afins) o valor é placeholder
            // POR DEFINIÇÃO — é o propósito do arquivo. Segue reportado, para o
            // humano conferir, mas em MEDIUM: como CRITICAL bloqueava a aplicação
            // e o arquivo é justamente o que documenta as variáveis, o pipeline
            // inteiro parava por causa de "suaSenhaSegura123".
            if (ehTemplateDeAmbiente(path)) {
                issues.add(issue(ReviewRule.HARDCODED_SECRET, ReviewCategory.SECURITY, ReviewSeverity.MEDIUM,
                        path, null, "Valor de exemplo em template de ambiente — confirmar que não é credencial real",
                        null, "Preferir valor vazio (VARIAVEL=) em arquivos .env de exemplo"));
            } else {
                issues.add(critical(ReviewRule.HARDCODED_SECRET, ReviewCategory.SECURITY, path, "Possível segredo hardcoded detectado"));
            }
        }
        if (CREDENTIAL_BEARER.matcher(content).find() || URL_WITH_CREDENTIALS.matcher(content).find()) {
            issues.add(critical(ReviewRule.HARDCODED_CREDENTIAL, ReviewCategory.SECURITY, path, "Possível credencial hardcoded detectada"));
        }
        if (HARDCODED_URL.matcher(content).find() && !URL_WITH_CREDENTIALS.matcher(content).find()) {
            issues.add(issue(ReviewRule.HARDCODED_URL, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "URL hardcoded encontrada", null, "Externalizar a URL para configuração"));
        }

        if (THREAD_SLEEP.matcher(content).find()) {
            issues.add(issue(ReviewRule.THREAD_SLEEP, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.HIGH,
                    path, null, "Uso de Thread.sleep detectado", "Thread.sleep(", "Substituir por espera explícita/condicional"));
        }
        if (CYPRESS_FIXED_WAIT.matcher(content).find() || PLAYWRIGHT_FIXED_WAIT.matcher(content).find()) {
            issues.add(issue(ReviewRule.FIXED_WAIT, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.MEDIUM,
                    path, null, "Espera fixa detectada", null, "Preferir espera baseada em condição"));
        }
        if (ROBOT_SLEEP.matcher(content).find()) {
            issues.add(issue(ReviewRule.SLEEP_USAGE, ReviewCategory.WAIT_STRATEGY, ReviewSeverity.MEDIUM,
                    path, null, "Uso de Sleep fixo detectado", null, "Preferir 'Wait Until Keyword Succeeds'"));
        }

        if (FRAGILE_SELECTOR.matcher(content).find()) {
            issues.add(issue(ReviewRule.FRAGILE_SELECTOR, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "Seletor potencialmente frágil detectado", null, "Preferir seletor semântico ou atributo de teste"));
        }

        if (artifact.componentType() == PlanComponentType.TEST && !ASSERTION_EVIDENCE.matcher(content).find()) {
            issues.add(issue(ReviewRule.MISSING_ASSERTION, ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                    path, null, "Arquivo de teste sem assertion aparente", null, "Adicionar assertion ao teste"));
        }

        if (EMPTY_BLOCK.matcher(content).find()) {
            ReviewRule emptyRule = artifact.componentType() == PlanComponentType.TEST ? ReviewRule.EMPTY_TEST : ReviewRule.EMPTY_METHOD;
            issues.add(issue(emptyRule, ReviewCategory.TEST_DESIGN, ReviewSeverity.MEDIUM,
                    path, null, "Bloco de código vazio detectado", null, "Implementar o corpo do método/teste"));
        }

        if (WILDCARD_IMPORT.matcher(content).find()) {
            issues.add(issue(ReviewRule.WILDCARD_IMPORT, ReviewCategory.IMPORT, ReviewSeverity.LOW,
                    path, null, "Import wildcard detectado", null, "Importar somente os símbolos necessários"));
        }

        if (GENERIC_EXCEPTION.matcher(content).find()) {
            issues.add(issue(ReviewRule.GENERIC_EXCEPTION, ReviewCategory.ERROR_HANDLING, ReviewSeverity.MEDIUM,
                    path, null, "Captura de exceção genérica detectada", null, "Capturar exceções específicas"));
        }

        if (hasDuplicatedLines(content)) {
            issues.add(issue(ReviewRule.DUPLICATED_CODE, ReviewCategory.DUPLICATION, ReviewSeverity.LOW,
                    path, null, "Linhas duplicadas detectadas", null, "Extrair trecho repetido para um helper"));
        }

        if (content.length() > FILE_TOO_LARGE_THRESHOLD) {
            issues.add(issue(ReviewRule.FILE_TOO_LARGE, ReviewCategory.MAINTAINABILITY, ReviewSeverity.LOW,
                    path, null, "Arquivo gerado muito grande", null, "Considerar dividir em arquivos menores"));
        }

        if (!matchesNamingConvention(framework, artifact.componentType(), path)) {
            issues.add(issue(ReviewRule.NAMING_CONVENTION_MISMATCH, ReviewCategory.NAMING, ReviewSeverity.LOW,
                    path, null, "Nome de arquivo fora do padrão esperado", null, "Ajustar nome do arquivo à convenção do framework"));
        }

        return issues;
    }

    private boolean isExtensionCompatibleWithLanguage(AutomationLanguage language, String extension) {
        if (language == null) return true;
        return switch (language) {
            case TYPESCRIPT -> Set.of("ts", "js", "json").contains(extension);
            case JAVASCRIPT -> Set.of("js", "json").contains(extension);
            case JAVA -> Set.of("java").contains(extension);
            case PYTHON -> Set.of("py").contains(extension);
            case ROBOT -> Set.of("robot", "resource").contains(extension);
            case CSHARP -> Set.of("cs").contains(extension);
            case UNKNOWN -> true;
        };
    }

    private boolean matchesNamingConvention(AutomationFramework framework, PlanComponentType componentType, String path) {
        if (componentType != PlanComponentType.TEST) return true;
        String fileName = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
        return switch (framework) {
            case PLAYWRIGHT, CYPRESS -> fileName.contains("spec") || fileName.contains("test") || fileName.contains("cy");
            case SELENIDE, SELENIUM, REST_ASSURED -> fileName.endsWith("test.java") || fileName.endsWith("tests.java") || fileName.endsWith("it.java");
            case ROBOT_FRAMEWORK -> true;
            default -> true;
        };
    }

    private boolean hasDuplicatedLines(String content) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.length() < MIN_DUPLICATE_LINE_LENGTH) continue;
            counts.merge(trimmed, 1, Integer::sum);
        }
        return counts.values().stream().anyMatch(count -> count >= DUPLICATED_LINE_THRESHOLD);
    }

    private String extensionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /**
     * test.skip / it.skip / describe.skip cuja condição envolve process.env,
     * direta ou por variável carregada dele logo acima. O ".*" limitado à
     * mesma linha evita alcançar um skip legítimo de outro trecho.
     */
    private static final Pattern SKIP_POR_ENV = Pattern.compile(
            "(?m)^.*\\b(?:test|it|describe)\\.skip\\s*\\([^)\\n]*"
                    + "(?:process\\.env|!\\s*[a-zA-Z_$][\\w$]*\\s*(?:\\|\\||,))[^)\\n]*\\).*$");

    /**
     * Status assertado, nas duas formas que aparecem na prática:
     * {@code toBe(401)} e {@code expect([405, 404]).toContain(status())}.
     * A segunda exige "status" na mesma linha — sem isso, qualquer literal de
     * três dígitos num array viraria falso positivo.
     */
    private static final Pattern STATUS_NO_MATCHER = Pattern.compile(
            "(?:toBe|toContain|toEqual)\\s*\\(\\s*([1-5][0-9]{2})\\b");
    private static final Pattern STATUS_EM_LISTA = Pattern.compile(
            "(?m)^.*\\bstatus\\b.*$");
    private static final Pattern TRES_DIGITOS = Pattern.compile("\\b([1-5][0-9]{2})\\b");

    private static final Pattern TO_MATCH_OBJECT =
            Pattern.compile("toMatchObject\\(\\s*\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern CHAVE_DE_OBJETO =
            Pattern.compile("([A-Za-z_][A-Za-z0-9_]*)\\s*:");
    private static final Pattern TO_EQUAL_ARRAY =
            Pattern.compile("toEqual\\(\\s*\\[([^\\]]*)\\]", Pattern.DOTALL);
    private static final Pattern STRING_LITERAL =
            Pattern.compile("[\"']([A-Za-z_][A-Za-z0-9_]*)[\"']");

    /**
     * Acusa campo assertado que o cenário nunca mencionou.
     *
     * <p>Observado em produção: dez testes gerados passaram em TODAS as regras
     * estáticas e asseriam um contrato inventado — "message" onde o contrato
     * dizia "mensagem". As regras olhavam a forma do teste, nenhuma olhava se
     * ele falava da API certa. O erro só apareceria na execução.
     *
     * <p>Lê apenas duas posições sintáticas onde o nome é inequivocamente um
     * campo de resposta: chaves de toMatchObject e literais de toEqual([...]).
     * Property access solto (body.x) ficou de fora de propósito — pegaria
     * response.status() e afins.
     */
    private List<ReviewIssue> reviewFidelidadeAoContrato(VocabularioDoContrato contrato,
                                                          GeneratedArtifactReader.ReadArtifact artifact) {
        if (contrato == null || !contrato.utilizavel()) {
            return List.of();
        }
        String conteudo = artifact.content();
        if (conteudo == null || conteudo.isBlank()) {
            return List.of();
        }

        List<String> assertados = new ArrayList<>();
        Matcher objeto = TO_MATCH_OBJECT.matcher(conteudo);
        while (objeto.find()) {
            Matcher chave = CHAVE_DE_OBJETO.matcher(objeto.group(1));
            while (chave.find()) {
                assertados.add(chave.group(1));
            }
        }
        Matcher lista = TO_EQUAL_ARRAY.matcher(conteudo);
        while (lista.find()) {
            Matcher literal = STRING_LITERAL.matcher(lista.group(1));
            while (literal.find()) {
                assertados.add(literal.group(1));
            }
        }

        List<String> desconhecidos = VocabularioDoContrato.nomesDesconhecidos(contrato, assertados);
        if (desconhecidos.isEmpty()) {
            return List.of();
        }
        return List.of(issue(ReviewRule.CONTRACT_FIELD_UNKNOWN, ReviewCategory.ASSERTION, ReviewSeverity.HIGH,
                artifact.relativePath(), null,
                "Teste assere campo que o cenário não menciona: " + String.join(", ", desconhecidos),
                String.join(", ", desconhecidos),
                "Conferir o nome contra o contrato do cenário. Campo inventado passa na revisão e falha na execução"));
    }

    /**
     * Teste que se PULA quando falta variável de ambiente.
     *
     * <p>Observado no código gerado: {@code test.skip(!usuario || !senha, ...)}.
     * Num CI sem as variáveis, a suíte reporta verde sem ter exercitado nada —
     * falso sucesso é pior que falha, porque ninguém investiga o verde.
     *
     * <p>A forma correta é falhar com a causa explícita: variável ausente é
     * defeito de configuração, não motivo para omitir a verificação.
     */
    private List<ReviewIssue> reviewSkipPorAmbiente(GeneratedArtifactReader.ReadArtifact artifact) {
        String conteudo = artifact.content();
        if (conteudo == null || conteudo.isBlank()) {
            return List.of();
        }
        Matcher matcher = SKIP_POR_ENV.matcher(conteudo);
        if (!matcher.find()) {
            return List.of();
        }
        return List.of(issue(ReviewRule.SKIP_POR_AMBIENTE, ReviewCategory.TEST_DESIGN, ReviewSeverity.HIGH,
                artifact.relativePath(), null,
                "Teste se auto-pula quando falta variável de ambiente — a suíte reporta verde sem testar nada",
                matcher.group().trim(),
                "Falhar com a causa explícita em vez de pular: variável ausente é erro de configuração"));
    }

    /**
     * Asserção de status HTTP que o cenário nunca definiu.
     *
     * <p>Caso real: o cenário tratava "método diferente de POST" como
     * exploratório e o teste gerado afirmou {@code toContain([405, 404])}. A
     * API responde 500 — o teste falharia por causa do palpite, não de um
     * defeito real, e o vermelho apontaria para o lugar errado.
     *
     * <p>Só acusa quando o cenário define status suficientes para servir de
     * referência; caso contrário se cala, como as demais regras derivadas.
     */
    private List<ReviewIssue> reviewStatusNaoDefinido(Set<String> statusDoCenario,
                                                       GeneratedArtifactReader.ReadArtifact artifact) {
        if (statusDoCenario == null || statusDoCenario.size() < 2) {
            return List.of();
        }
        String conteudo = artifact.content();
        if (conteudo == null || conteudo.isBlank()) {
            return List.of();
        }

        List<String> inventados = new ArrayList<>();

        Matcher noMatcher = STATUS_NO_MATCHER.matcher(conteudo);
        while (noMatcher.find()) {
            acusarSeDesconhecido(noMatcher.group(1), statusDoCenario, inventados);
        }

        Matcher linhasComStatus = STATUS_EM_LISTA.matcher(conteudo);
        while (linhasComStatus.find()) {
            Matcher numeros = TRES_DIGITOS.matcher(linhasComStatus.group());
            while (numeros.find()) {
                acusarSeDesconhecido(numeros.group(1), statusDoCenario, inventados);
            }
        }
        if (inventados.isEmpty()) {
            return List.of();
        }
        return List.of(issue(ReviewRule.ASSERCAO_SOBRE_COMPORTAMENTO_EXPLORATORIO, ReviewCategory.ASSERTION,
                ReviewSeverity.HIGH, artifact.relativePath(), null,
                "Teste assere status HTTP que o cenário não define: " + String.join(", ", inventados),
                String.join(", ", inventados),
                "Conferir o status contra o cenário. Comportamento não especificado vira suposição e falha na execução"));
    }

    private void acusarSeDesconhecido(String status, Set<String> statusDoCenario, List<String> inventados) {
        if (!statusDoCenario.contains(status) && !inventados.contains(status)) {
            inventados.add(status);
        }
    }

    private ReviewIssue issue(ReviewRule rule, ReviewCategory category, ReviewSeverity severity, String relativePath,
                               Integer line, String message, String evidence, String recommendation) {
        return new ReviewIssue(rule.name(), category, severity, relativePath, line, message, evidence, recommendation,
                severity == ReviewSeverity.CRITICAL);
    }

    private ReviewIssue critical(ReviewRule rule, ReviewCategory category, String relativePath, String message) {
        return issue(rule, category, ReviewSeverity.CRITICAL, relativePath, null, message, null,
                "Revisar imediatamente - risco de segurança");
    }
}
