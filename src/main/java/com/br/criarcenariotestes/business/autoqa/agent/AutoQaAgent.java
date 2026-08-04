package com.br.criarcenariotestes.business.autoqa.agent;

import com.br.criarcenariotestes.business.autoqa.context.AutoQaContext;
import com.br.criarcenariotestes.business.autoqa.model.AgentExecutionResult;

public interface AutoQaAgent {

    String getName();

    AgentExecutionResult execute(AutoQaContext context);
}
