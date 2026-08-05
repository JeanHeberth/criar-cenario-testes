package com.br.criarcenariotestes.business.autoqa.knowledge.classifier;

import com.br.criarcenariotestes.business.autoqa.knowledge.parser.SourceMetadataParser;
import com.br.criarcenariotestes.business.autoqa.model.discovery.ProjectDiscoveryResult;
import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;

public interface ProjectComponentClassifier {

    boolean supports(ProjectDiscoveryResult discovery);

    ProjectComponent classify(ProjectDiscoveryResult discovery, SourceMetadataParser.SourceMetadata metadata);
}
