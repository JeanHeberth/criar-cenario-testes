package com.br.criarcenariotestes.business.autoqa.knowledge;

import com.br.criarcenariotestes.business.autoqa.knowledge.builder.ProjectKnowledgeResultBuilder;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.GenericComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.classifier.ProjectComponentClassifier;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.ResourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.NamingConventionResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ProjectStructureResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.resolver.ReuseCandidateResolver;
import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanResult;
import com.br.criarcenariotestes.business.autoqa.knowledge.scanner.KnowledgeScanner;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.KnowledgeStatus;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectKnowledgeResult;
import com.br.criarcenariotestes.business.autoqa.model.scenario.ScenarioAnalysisResult;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ProjectKnowledgeService {

    private final KnowledgeScanner scanner;
    private final List<SourceMetadataParser> parsers;
    private final List<ProjectComponentClassifier> classifiers;
    private final ProjectStructureResolver structureResolver;
    private final NamingConventionResolver namingConventionResolver;
    private final ReuseCandidateResolver reuseCandidateResolver;
    private final ProjectKnowledgeResultBuilder builder;

    public ProjectKnowledgeService(KnowledgeScanner scanner,
                                   List<SourceMetadataParser> parsers,
                                   List<ProjectComponentClassifier> classifiers,
                                   ProjectStructureResolver structureResolver,
                                   NamingConventionResolver namingConventionResolver,
                                   ReuseCandidateResolver reuseCandidateResolver,
                                   ProjectKnowledgeResultBuilder builder) {
        this.scanner = Objects.requireNonNull(scanner, "scanner must not be null");
        this.parsers = sort(Objects.requireNonNull(parsers, "parsers must not be null"));
        this.classifiers = sort(Objects.requireNonNull(classifiers, "classifiers must not be null"));
        this.structureResolver = Objects.requireNonNull(structureResolver, "structureResolver must not be null");
        this.namingConventionResolver = Objects.requireNonNull(namingConventionResolver, "namingConventionResolver must not be null");
        this.reuseCandidateResolver = Objects.requireNonNull(reuseCandidateResolver, "reuseCandidateResolver must not be null");
        this.builder = Objects.requireNonNull(builder, "builder must not be null");
    }

    public ProjectKnowledgeResult collect(ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenarioAnalysis) {
        validateInput(discovery, scenarioAnalysis);

        KnowledgeScanResult scanResult = scanner.scan(discovery.getNormalizedProjectPath());
        List<ProjectComponent> components = new ArrayList<>();
        for (KnowledgeScanResult.KnowledgeFile file : scanResult.files()) {
            SourceMetadataParser parser = findParser(file);
            if (parser == null) {
                continue;
            }
            SourceMetadataParser.SourceMetadata metadata = parser.parse(file);
            ProjectComponentClassifier classifier = selectClassifier(discovery, metadata);
            ProjectComponent component = classifier.classify(discovery, metadata);
            if (component.type() == ComponentType.UNKNOWN && !(classifier instanceof GenericComponentClassifier)) {
                component = selectGenericClassifier().classify(discovery, metadata);
            }
            components.add(component);
        }

        ProjectStructureResolver.ProjectStructure structure = structureResolver.resolve(scanResult, components);
        var namingConvention = namingConventionResolver.resolve(components);
        var reuseCandidates = reuseCandidateResolver.resolve(components, scenarioAnalysis);
        KnowledgeStatus status = resolveStatus(discovery, scanResult, components);
        boolean valid = status != KnowledgeStatus.FAILED;

        return builder.build(
                discovery.getNormalizedProjectPath(),
                components,
                reuseCandidates,
                namingConvention,
                structure,
                mergeWarnings(scanResult.warnings(), components),
                status,
                valid
        );
    }

    private KnowledgeStatus resolveStatus(ProjectDiscoveryResult discovery, KnowledgeScanResult scanResult, List<ProjectComponent> components) {
        if (scanResult.truncated()) {
            return discovery.getAutomationFramework() == AutomationFramework.UNKNOWN ? KnowledgeStatus.PARTIAL : KnowledgeStatus.PARTIAL;
        }
        if (components.isEmpty()) {
            return KnowledgeStatus.EMPTY;
        }
        boolean unknownFramework = discovery.getAutomationFramework() == AutomationFramework.UNKNOWN;
        boolean hasUnknownComponent = components.stream().anyMatch(component -> component.type() == ComponentType.UNKNOWN);
        if (unknownFramework || hasUnknownComponent || !scanResult.warnings().isEmpty()) {
            return KnowledgeStatus.PARTIAL;
        }
        return KnowledgeStatus.COMPLETE;
    }

    private List<String> mergeWarnings(List<String> scanWarnings, List<ProjectComponent> components) {
        List<String> warnings = new ArrayList<>(scanWarnings);
        for (ProjectComponent component : components) {
            warnings.addAll(component.warnings());
        }
        return warnings.stream().distinct().sorted().toList();
    }

    private SourceMetadataParser findParser(KnowledgeScanResult.KnowledgeFile file) {
        return parsers.stream().filter(parser -> parser.supports(file)).findFirst().orElse(null);
    }

    private ProjectComponentClassifier selectClassifier(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata) {
        for (ProjectComponentClassifier classifier : classifiers) {
            if (classifier instanceof GenericComponentClassifier) {
                continue;
            }
            if (classifier.supports(discovery)) {
                return classifier;
            }
        }
        return selectGenericClassifier();
    }

    private ProjectComponentClassifier selectGenericClassifier() {
        return classifiers.stream()
                .filter(GenericComponentClassifier.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new ProjectKnowledgeException("GenericComponentClassifier missing"));
    }

    private void validateInput(ProjectDiscoveryResult discovery, ScenarioAnalysisResult scenarioAnalysis) {
        if (discovery == null) {
            throw new ProjectKnowledgeValidationException("discovery must not be null");
        }
        if (scenarioAnalysis == null) {
            throw new ProjectKnowledgeValidationException("scenarioAnalysis must not be null");
        }
        if (discovery.getNormalizedProjectPath() == null) {
            throw new ProjectKnowledgeValidationException("normalizedProjectPath must not be null");
        }
    }

    private <T> List<T> sort(List<T> values) {
        List<T> copy = new ArrayList<>(values);
        AnnotationAwareOrderComparator.sort(copy);
        return List.copyOf(copy);
    }
}
