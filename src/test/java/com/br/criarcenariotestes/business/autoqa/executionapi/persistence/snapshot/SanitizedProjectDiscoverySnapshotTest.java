package com.br.criarcenariotestes.business.autoqa.executionapi.persistence.snapshot;

import com.br.criarcenariotestes.business.autoqa.model.discovery.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SanitizedProjectDiscoverySnapshot - Testes Unitários")
class SanitizedProjectDiscoverySnapshotTest {

    @Test
    @DisplayName("from() nunca deve capturar o normalizedProjectPath")
    void fromNuncaCapturaProjectPath() {
        ProjectDiscoveryResult original = discoveryComPath("/projeto/sensivel/secreto");

        SanitizedProjectDiscoverySnapshot snapshot = SanitizedProjectDiscoverySnapshot.from(original);

        assertThat(snapshot.toString()).doesNotContain("/projeto/sensivel/secreto");
    }

    @Test
    @DisplayName("toResult() deve reconstruir um ProjectDiscoveryResult equivalente, usando o path informado externamente")
    void toResultDeveReconstruirComPathExterno() {
        ProjectDiscoveryResult original = discoveryComPath("/projeto/original");
        SanitizedProjectDiscoverySnapshot snapshot = SanitizedProjectDiscoverySnapshot.from(original);

        ProjectDiscoveryResult reconstructed = snapshot.toResult(Path.of("/projeto/reidratado"));

        assertThat(reconstructed.normalizedProjectPath()).isEqualTo(Path.of("/projeto/reidratado"));
        assertThat(reconstructed.automationFramework()).isEqualTo(original.automationFramework());
        assertThat(reconstructed.language()).isEqualTo(original.language());
        assertThat(reconstructed.packageManager()).isEqualTo(original.packageManager());
        assertThat(reconstructed.buildTool()).isEqualTo(original.buildTool());
        assertThat(reconstructed.testingFrameworks()).isEqualTo(original.testingFrameworks());
        assertThat(reconstructed.detectedFrameworks()).isEqualTo(original.detectedFrameworks());
        assertThat(reconstructed.libraries()).isEqualTo(original.libraries());
        assertThat(reconstructed.configurationFile()).isEqualTo(original.configurationFile());
        assertThat(reconstructed.evidenceFiles()).isEqualTo(original.evidenceFiles());
        assertThat(reconstructed.warnings()).isEqualTo(original.warnings());
        assertThat(reconstructed.confidence()).isEqualTo(original.confidence());
        assertThat(reconstructed.valid()).isEqualTo(original.valid());
    }

    @Test
    @DisplayName("Reconstrução é determinística para o mesmo snapshot e mesmo path")
    void reconstrucaoEhDeterministica() {
        SanitizedProjectDiscoverySnapshot snapshot = SanitizedProjectDiscoverySnapshot.from(discoveryComPath("/p"));
        Path path = Path.of("/reidratado");

        assertThat(snapshot.toResult(path)).isEqualTo(snapshot.toResult(path));
    }

    private ProjectDiscoveryResult discoveryComPath(String path) {
        return new ProjectDiscoveryResult(Path.of(path), AutomationFramework.PLAYWRIGHT, AutomationLanguage.TYPESCRIPT,
                PackageManager.NPM, BuildTool.NPM, Set.of(), Set.of(AutomationFramework.PLAYWRIGHT), List.of("lib1"),
                "playwright.config.ts", List.of("package.json"), List.of(), DiscoveryConfidence.HIGH, true);
    }
}
