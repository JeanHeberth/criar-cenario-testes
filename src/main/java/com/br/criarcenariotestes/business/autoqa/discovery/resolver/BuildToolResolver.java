package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.BuildTool;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import org.springframework.stereotype.Component;

@Component
public class BuildToolResolver {

    public BuildTool resolve(ParsedProjectFiles parsedProjectFiles, PackageManager packageManager) {
        if (parsedProjectFiles.mavenPom() && parsedProjectFiles.gradleBuild()) {
            return BuildTool.UNKNOWN;
        }
        if (parsedProjectFiles.mavenPom()) {
            return BuildTool.MAVEN;
        }
        if (parsedProjectFiles.gradleBuild()) {
            return BuildTool.GRADLE;
        }
        if (isRobotFrameworkProject(parsedProjectFiles)) {
            return BuildTool.ROBOT;
        }
        if (packageManager == PackageManager.NPM) {
            return BuildTool.NPM;
        }
        if (packageManager == PackageManager.YARN) {
            return BuildTool.YARN;
        }
        if (packageManager == PackageManager.PNPM) {
            return BuildTool.PNPM;
        }
        return BuildTool.UNKNOWN;
    }

    private boolean isRobotFrameworkProject(ParsedProjectFiles parsedProjectFiles) {
        return parsedProjectFiles.robotYaml()
                || !parsedProjectFiles.robotFiles().isEmpty()
                || parsedProjectFiles.requirementsContents().stream().anyMatch(content -> content.contains("robotframework"))
                || parsedProjectFiles.pyprojectContents().stream().anyMatch(content -> content.contains("robotframework"))
                || parsedProjectFiles.poetryContents().stream().anyMatch(content -> content.contains("robotframework"));
    }
}
