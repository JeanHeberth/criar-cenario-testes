package com.br.criarcenariotestes.business.autoqa.service;

import com.br.criarcenariotestes.business.autoqa.model.enums.AutomationFramework;
import com.br.criarcenariotestes.business.autoqa.model.enums.PackageManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CommandPolicyService {

    public List<String> validationCommand(AutomationFramework framework, PackageManager packageManager) {
        return switch (packageManager != null ? packageManager : PackageManager.UNKNOWN) {
            case NPM -> List.of(resolveNpmExecutable(), "exec", "tsc", "--", "--noEmit");
            case YARN -> List.of(resolveYarnExecutable(), "tsc", "--noEmit");
            case PNPM -> List.of(resolvePnpmExecutable(), "exec", "tsc", "--noEmit");
            case UNKNOWN -> List.of(resolveNpxExecutable(), "tsc", "--noEmit");
        };
    }

    public List<String> testCommand(AutomationFramework framework, PackageManager packageManager, String specFile) {
        String safeSpec = (specFile != null && !specFile.isBlank()) ? specFile : "";

        if (framework == AutomationFramework.CYPRESS) {
            return switch (packageManager != null ? packageManager : PackageManager.UNKNOWN) {
                case NPM, UNKNOWN -> safeSpec.isBlank()
                        ? List.of(resolveNpxExecutable(), "cypress", "run")
                        : List.of(resolveNpxExecutable(), "cypress", "run", "--spec", safeSpec);
                case YARN -> safeSpec.isBlank()
                        ? List.of(resolveYarnExecutable(), "cypress", "run")
                        : List.of(resolveYarnExecutable(), "cypress", "run", "--spec", safeSpec);
                case PNPM -> safeSpec.isBlank()
                        ? List.of(resolvePnpmExecutable(), "exec", "cypress", "run")
                        : List.of(resolvePnpmExecutable(), "exec", "cypress", "run", "--spec", safeSpec);
            };
        }
        return switch (packageManager != null ? packageManager : PackageManager.UNKNOWN) {
            case NPM, UNKNOWN -> safeSpec.isBlank()
                    ? List.of(resolveNpxExecutable(), "playwright", "test")
                    : List.of(resolveNpxExecutable(), "playwright", "test", safeSpec);
            case YARN -> safeSpec.isBlank()
                    ? List.of(resolveYarnExecutable(), "playwright", "test")
                    : List.of(resolveYarnExecutable(), "playwright", "test", safeSpec);
            case PNPM -> safeSpec.isBlank()
                    ? List.of(resolvePnpmExecutable(), "exec", "playwright", "test")
                    : List.of(resolvePnpmExecutable(), "exec", "playwright", "test", safeSpec);
        };
    }

    private String resolveNpxExecutable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "npx.cmd" : "npx";
    }

    private String resolveNpmExecutable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "npm.cmd" : "npm";
    }

    private String resolveYarnExecutable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "yarn.cmd" : "yarn";
    }

    private String resolvePnpmExecutable() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win") ? "pnpm.cmd" : "pnpm";
    }
}
