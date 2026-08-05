package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ComponentType;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.SourceLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GenericComponentClassifier - Testes Unitários")
class GenericComponentClassifierTest {

    private final GenericComponentClassifier classifier = new GenericComponentClassifier();

    @Test
    @DisplayName("Deve classificar por extensão e diretório")
    void deveClassificarPorExtensaoEDiretorio() {
        ProjectComponent component = classifier.classify(null, metadata("src/pages/LoginPage.ts", "LoginPage", List.of("PAGE_OBJECT_EVIDENCE"), false, List.of("open")));

        assertThat(component.type()).isEqualTo(ComponentType.PAGE_OBJECT);
    }

    @Test
    @DisplayName("Deve retornar UNKNOWN quando não há evidência")
    void deveRetornarUnknownQuandoNaoHaEvidencia() {
        ProjectComponent component = classifier.classify(null, metadata("src/misc/Thing.txt", "Thing", List.of(), false, List.of()));

        assertThat(component.type()).isEqualTo(ComponentType.UNKNOWN);
    }

    @Test
    @DisplayName("Deve não criar falso positivo agressivo")
    void deveNaoCriarFalsoPositivoAgressivo() {
        ProjectComponent component = classifier.classify(null, metadata("src/random/Util.ts", "Util", List.of(), false, List.of()));

        assertThat(component.type()).isEqualTo(ComponentType.UNKNOWN);
    }

    private SourceMetadataParser.SourceMetadata metadata(String path, String name, List<String> tags, boolean testComponent, List<String> methods) {
        return new SourceMetadataParser.SourceMetadata(path, name, SourceLanguage.TYPESCRIPT, null, List.of(), methods, List.of(), List.of(), List.of(), tags, testComponent, List.of());
    }
}
