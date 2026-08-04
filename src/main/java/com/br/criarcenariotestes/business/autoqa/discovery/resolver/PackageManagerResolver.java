package com.br.criarcenariotestes.business.autoqa.discovery.resolver;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.PackageManager;
import org.springframework.stereotype.Component;

@Component
public class PackageManagerResolver {

    public PackageManager resolve(ParsedProjectFiles parsedProjectFiles) {
        if (parsedProjectFiles.packageManagerCandidates().size() > 1) {
            return PackageManager.UNKNOWN;
        }
        if (parsedProjectFiles.packageManagerCandidates().isEmpty()) {
            if (parsedProjectFiles.requirementsTxt()) {
                return PackageManager.PIP;
            }
            return PackageManager.UNKNOWN;
        }
        return parsedProjectFiles.packageManagerCandidates().iterator().next();
    }
}
