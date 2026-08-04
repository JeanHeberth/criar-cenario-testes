package com.br.criarcenariotestes.business.autoqa.discovery.parser;

import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record ParsedProjectFiles(
        List<String> evidenceFiles,
        List<String> warnings,
        Set<String> nodeDependencies,
        Set<String> mavenDependencies,
        List<String> gradleContents,
        List<String> requirementsContents,
        List<String> pyprojectContents,
        List<String> poetryContents,
        Set<PackageManager> packageManagerCandidates,
        boolean packageJson,
        boolean mavenPom,
        boolean gradleBuild,
        boolean requirementsTxt,
        boolean pyprojectToml,
        boolean poetryLock,
        boolean robotYaml,
        boolean tsconfig,
        boolean packageLock,
        boolean yarnLock,
        boolean pnpmLock,
        String packageJsonPath,
        String mavenPomPath,
        String gradleBuildPath,
        String requirementsPath,
        String pyprojectPath,
        String poetryLockPath,
        String robotYamlPath,
        String playwrightConfig,
        String cypressConfig,
        boolean playwrightConfigIsTs,
        boolean cypressConfigIsTs,
        Set<String> robotFiles
) {
    public ParsedProjectFiles {
        evidenceFiles = evidenceFiles == null ? List.of() : List.copyOf(evidenceFiles);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        nodeDependencies = nodeDependencies == null ? Set.of() : Set.copyOf(nodeDependencies);
        mavenDependencies = mavenDependencies == null ? Set.of() : Set.copyOf(mavenDependencies);
        gradleContents = gradleContents == null ? List.of() : List.copyOf(gradleContents);
        requirementsContents = requirementsContents == null ? List.of() : List.copyOf(requirementsContents);
        pyprojectContents = pyprojectContents == null ? List.of() : List.copyOf(pyprojectContents);
        poetryContents = poetryContents == null ? List.of() : List.copyOf(poetryContents);
        packageManagerCandidates = packageManagerCandidates == null ? Set.of() : Set.copyOf(packageManagerCandidates);
        robotFiles = robotFiles == null ? Set.of() : Set.copyOf(robotFiles);
    }

    public static final class Builder {
        private final List<String> evidenceFiles = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final Set<String> nodeDependencies = new LinkedHashSet<>();
        private final Set<String> mavenDependencies = new LinkedHashSet<>();
        private final List<String> gradleContents = new ArrayList<>();
        private final List<String> requirementsContents = new ArrayList<>();
        private final List<String> pyprojectContents = new ArrayList<>();
        private final List<String> poetryContents = new ArrayList<>();
        private final Set<PackageManager> packageManagerCandidates = new LinkedHashSet<>();
        private final Set<String> robotFiles = new LinkedHashSet<>();
        private boolean packageJson;
        private boolean mavenPom;
        private boolean gradleBuild;
        private boolean requirementsTxt;
        private boolean pyprojectToml;
        private boolean poetryLock;
        private boolean robotYaml;
        private boolean tsconfig;
        private boolean packageLock;
        private boolean yarnLock;
        private boolean pnpmLock;
        private String packageJsonPath;
        private String mavenPomPath;
        private String gradleBuildPath;
        private String requirementsPath;
        private String pyprojectPath;
        private String poetryLockPath;
        private String robotYamlPath;
        private String playwrightConfig;
        private String cypressConfig;
        private boolean playwrightConfigIsTs;
        private boolean cypressConfigIsTs;

        public ParsedProjectFiles build() {
            return new ParsedProjectFiles(
                    evidenceFiles, warnings, nodeDependencies, mavenDependencies, gradleContents,
                    requirementsContents, pyprojectContents, poetryContents, packageManagerCandidates,
                    packageJson, mavenPom, gradleBuild, requirementsTxt, pyprojectToml, poetryLock, robotYaml,
                    tsconfig, packageLock, yarnLock, pnpmLock, packageJsonPath, mavenPomPath, gradleBuildPath,
                    requirementsPath, pyprojectPath, poetryLockPath, robotYamlPath, playwrightConfig, cypressConfig,
                    playwrightConfigIsTs, cypressConfigIsTs, robotFiles
            );
        }

        public List<String> evidenceFiles() { return evidenceFiles; }
        public List<String> warnings() { return warnings; }
        public Set<String> nodeDependencies() { return nodeDependencies; }
        public Set<String> mavenDependencies() { return mavenDependencies; }
        public List<String> gradleContents() { return gradleContents; }
        public List<String> requirementsContents() { return requirementsContents; }
        public List<String> pyprojectContents() { return pyprojectContents; }
        public List<String> poetryContents() { return poetryContents; }
        public Set<PackageManager> packageManagerCandidates() { return packageManagerCandidates; }
        public Set<String> robotFiles() { return robotFiles; }
        public boolean packageJson() { return packageJson; }
        public void packageJson(boolean value) { this.packageJson = value; }
        public boolean mavenPom() { return mavenPom; }
        public void mavenPom(boolean value) { this.mavenPom = value; }
        public boolean gradleBuild() { return gradleBuild; }
        public void gradleBuild(boolean value) { this.gradleBuild = value; }
        public boolean requirementsTxt() { return requirementsTxt; }
        public void requirementsTxt(boolean value) { this.requirementsTxt = value; }
        public boolean pyprojectToml() { return pyprojectToml; }
        public void pyprojectToml(boolean value) { this.pyprojectToml = value; }
        public boolean poetryLock() { return poetryLock; }
        public void poetryLock(boolean value) { this.poetryLock = value; }
        public boolean robotYaml() { return robotYaml; }
        public void robotYaml(boolean value) { this.robotYaml = value; }
        public boolean tsconfig() { return tsconfig; }
        public void tsconfig(boolean value) { this.tsconfig = value; }
        public boolean packageLock() { return packageLock; }
        public void packageLock(boolean value) { this.packageLock = value; }
        public boolean yarnLock() { return yarnLock; }
        public void yarnLock(boolean value) { this.yarnLock = value; }
        public boolean pnpmLock() { return pnpmLock; }
        public void pnpmLock(boolean value) { this.pnpmLock = value; }
        public String packageJsonPath() { return packageJsonPath; }
        public void packageJsonPath(String value) { this.packageJsonPath = value; }
        public String mavenPomPath() { return mavenPomPath; }
        public void mavenPomPath(String value) { this.mavenPomPath = value; }
        public String gradleBuildPath() { return gradleBuildPath; }
        public void gradleBuildPath(String value) { this.gradleBuildPath = value; }
        public String requirementsPath() { return requirementsPath; }
        public void requirementsPath(String value) { this.requirementsPath = value; }
        public String pyprojectPath() { return pyprojectPath; }
        public void pyprojectPath(String value) { this.pyprojectPath = value; }
        public String poetryLockPath() { return poetryLockPath; }
        public void poetryLockPath(String value) { this.poetryLockPath = value; }
        public String robotYamlPath() { return robotYamlPath; }
        public void robotYamlPath(String value) { this.robotYamlPath = value; }
        public String playwrightConfig() { return playwrightConfig; }
        public void playwrightConfig(String value) { this.playwrightConfig = value; }
        public String cypressConfig() { return cypressConfig; }
        public void cypressConfig(String value) { this.cypressConfig = value; }
        public boolean playwrightConfigIsTs() { return playwrightConfigIsTs; }
        public void playwrightConfigIsTs(boolean value) { this.playwrightConfigIsTs = value; }
        public boolean cypressConfigIsTs() { return cypressConfigIsTs; }
        public void cypressConfigIsTs(boolean value) { this.cypressConfigIsTs = value; }
    }
}
