package com.br.criarcenariotestes.business.autoqa.model.apply;

import com.br.criarcenariotestes.business.autoqa.model.generation.GeneratedFileOperation;

public enum ApplyOperation {
    CREATE, UPDATE, REUSE, NONE;

    public static ApplyOperation from(GeneratedFileOperation operation) {
        return switch (operation) {
            case CREATE -> CREATE;
            case UPDATE -> UPDATE;
            case REUSE -> REUSE;
            case NONE -> NONE;
        };
    }
}
