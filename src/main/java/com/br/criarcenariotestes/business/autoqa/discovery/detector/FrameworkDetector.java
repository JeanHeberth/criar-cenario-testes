package com.br.criarcenariotestes.business.autoqa.discovery.detector;

import com.br.criarcenariotestes.business.autoqa.discovery.parser.ParsedProjectFiles;
import com.br.criarcenariotestes.business.autoqa.model.discovery.AutomationFramework;

public interface FrameworkDetector {

    AutomationFramework framework();

    FrameworkDetection detect(ParsedProjectFiles project);
}
