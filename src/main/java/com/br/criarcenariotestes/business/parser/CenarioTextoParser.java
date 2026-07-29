package com.br.criarcenariotestes.business.parser;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CenarioTextoParser {

    private static final Pattern NOME_CAMPO_PATTERN = Pattern.compile("(?i)\\b(?:nome|objetivo|pré-condições|pré-condição|precondição|passos|resultado esperado)\\b");

    public List<CenarioItem> parsear(String resposta) {
        List<CenarioItem> itens = new ArrayList<>();

        if (resposta == null || resposta.isBlank()) {
            return itens;
        }

        String[] blocos = resposta.split("(?m)^\\s*---+\\s*$");

        for (String bloco : blocos) {
            String texto = bloco.trim();
            if (texto.isBlank()) {
                continue;
            }

            if (!NOME_CAMPO_PATTERN.matcher(texto).find()) {
                continue;
            }

            String nome = extrairCampoMultiplo(texto, "Nome");
            String objetivo = extrairCampoMultiplo(texto, "Objetivo");
            String precondicao = extrairCampoMultiplo(texto, "Pré-condições", "Precondição", "Pré-condição");
            String passos = extrairCampoMultiplo(texto, "Passos", "Script de Teste \\(Passo-a-Passo\\)");
            String resultadoEsperado = extrairCampoMultiplo(texto, "Resultado esperado", "Script de Teste \\(Passo-a-Passo\\) - Resultado");

            if (nome.isBlank() && objetivo.isBlank() && precondicao.isBlank() && passos.isBlank() && resultadoEsperado.isBlank()) {
                String textoLimpo = limparTextoParaParse(texto);
                if (!textoLimpo.isBlank()) {
                    nome = extrairPrimeiraLinha(textoLimpo);
                    objetivo = extrairTextoEntre(textoLimpo, "Objetivo", "Pré-condições", "Passos", "Resultado esperado", "Tipo", "Prioridade");
                    precondicao = extrairTextoEntre(textoLimpo, "Pré-condições", "Passos", "Resultado esperado", "Tipo", "Prioridade");
                    passos = extrairTextoEntre(textoLimpo, "Passos", "Resultado esperado", "Tipo", "Prioridade");
                    resultadoEsperado = extrairTextoEntre(textoLimpo, "Resultado esperado", "Tipo", "Prioridade");
                }
            }

            if (nome.isBlank() && objetivo.isBlank() && precondicao.isBlank() && passos.isBlank() && resultadoEsperado.isBlank()) {
                continue;
            }

            if (nome.isBlank()) {
                nome = extrairPrimeiraLinha(texto);
            }

            CenarioItem item = CenarioItem.builder()
                    .nome(nome)
                    .objetivo(objetivo)
                    .precondicao(precondicao)
                    .scriptTeste(passos)
                    .resultadoEsperado(resultadoEsperado)
                    .variaveis(valorOuPadrao(extrairCampo(texto, "Variáveis"), "Não se aplica"))
                    .componente(extrairCampo(texto, "Componente"))
                    .rotulos(extrairCampo(texto, "Rótulos"))
                    .proposito(valorOuPadrao(extrairCampo(texto, "Propósito"), "TESTE MANUAL"))
                    .pasta(extrairCampo(texto, "Pasta"))
                    .proprietario("JIRAUSER23105")
                    .cobertura(extrairCampo(texto, "Cobertura"))
                    .status(valorOuPadrao(extrairCampo(texto, "Status"), "APPROVED"))
                    .build();

            if (item.getNome() != null && !item.getNome().isBlank()) {
                itens.add(item);
            }
        }

        return itens;
    }

    public String extrairCriterios(String texto) {
        return extrairSecao(texto, "## 1. PLANO MACRO DE TESTE", "## 2. CENÁRIOS");
    }

    private String extrairCampo(String bloco, String campo) {
        String[] campos = {
                "Nome",
                "Objetivo",
                "Pré-condições",
                "Pré-condição",
                "Precondição",
                "Passos",
                "Script de Teste \\(Passo-a-Passo\\)",
                "Resultado esperado",
                "Script de Teste \\(Passo-a-Passo\\) - Resultado",
                "Tipo",
                "Prioridade",
                "Tags",
                "Variáveis",
                "Componente",
                "Rótulos",
                "Propósito",
                "Pasta",
                "Proprietário",
                "Cobertura",
                "Status"
        };

        int indice = -1;
        for (int i = 0; i < campos.length; i++) {
            if (campos[i].equals(campo)) {
                indice = i;
                break;
            }
        }

        if (indice == -1) {
            return "";
        }

        String[] proximos = java.util.Arrays.copyOfRange(campos, indice + 1, campos.length);
        String nextLabels = java.util.Arrays.stream(proximos)
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");

        String regex = "(?im)^\\s*(?:\\*\\*\\s*)?" + Pattern.quote(campo) + "(?:\\s*\\*\\*)?\\s*:\\s*(.+?)(?=^\\s*(?:\\*\\*\\s*)?(?:" + nextLabels + ")(?:\\s*\\*\\*)?\\s*:|^\\s*---|\\Z)";

        var matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(bloco);
        if (matcher.find()) {
            return limparTextoParaParse(matcher.group(1)).trim();
        }

        String fallbackRegex = "(?i)" + Pattern.quote(campo) + "\\s*:\\s*([\\s\\S]*?)(?=\\n(?:(?:" + nextLabels.replace("|", ")|(?:") + ")):|$)";
        var fallbackMatcher = Pattern.compile(fallbackRegex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(bloco);
        return fallbackMatcher.find() ? limparTextoParaParse(fallbackMatcher.group(1)).trim() : "";
    }

    private String extrairSecao(String texto, String inicio, String fim) {
        String lower = texto.toLowerCase();
        int idxInicio = lower.indexOf(inicio.toLowerCase());
        int idxFim = lower.indexOf(fim.toLowerCase());

        if (idxInicio != -1 && idxFim != -1 && idxInicio < idxFim) {
            return texto.substring(idxInicio + inicio.length(), idxFim).trim();
        }

        return "";
    }
    
    private String extrairCampoMultiplo(String bloco, String... possiveisNomes) {
        for (String nome : possiveisNomes) {
            String valor = extrairCampo(bloco, nome);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return "";
    }
    
    private String extrairTextoEntre(String texto, String inicio, String... possiveisFins) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        
        int idxInicio = texto.indexOf(inicio);
        if (idxInicio == -1) {
            return "";
        }
        
        idxInicio += inicio.length();
        
        int idxFim = texto.length();
        for (String fim : possiveisFins) {
            int idx = texto.indexOf(fim, idxInicio);
            if (idx != -1 && idx < idxFim) {
                idxFim = idx;
            }
        }
        
        return texto.substring(idxInicio, idxFim).trim();
    }
    
    private String extrairTextoAte(String texto, String... possiveisFins) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        
        int idxFim = texto.length();
        for (String fim : possiveisFins) {
            int idx = texto.indexOf(fim);
            if (idx != -1 && idx < idxFim) {
                idxFim = idx;
            }
        }
        
        return texto.substring(0, idxFim).trim();
    }

    private String limparTextoParaParse(String texto) {
        if (texto == null) {
            return "";
        }

        return texto
                .replace("\r", "")
                .replace("**", "")
                .replace("``", "")
                .replace("`", "")
                .replace("\n", "\n")
                .trim();
    }

    private String extrairPrimeiraLinha(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }

        String linha = texto.lines()
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .findFirst()
                .orElse("");

        return limparTextoParaParse(linha)
                .replace("^", "")
                .replace("#", "")
                .trim();
    }

    private String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}