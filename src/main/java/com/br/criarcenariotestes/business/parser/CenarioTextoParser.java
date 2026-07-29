package com.br.criarcenariotestes.business.parser;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CenarioTextoParser {

    private static final Pattern NOME_CAMPO_PATTERN = Pattern.compile("(?i)\\bnome\\s*:");

    public List<CenarioItem> parsear(String resposta) {
        List<CenarioItem> itens = new ArrayList<>();

        if (resposta == null || resposta.isBlank()) {
            return itens;
        }

        String[] blocos = resposta.split("(?m)^\\s*---+\\s*$");

        for (String bloco : blocos) {
            String texto = bloco.trim();

            if (!NOME_CAMPO_PATTERN.matcher(texto).find()) {
                continue;
            }

            // Extrair campos com múltiplos formatos possíveis
            String nome = extrairCampoMultiplo(texto, "Nome");
            String objetivo = extrairCampoMultiplo(texto, "Objetivo");
            String precondicao = extrairCampoMultiplo(texto, "Pré-condições", "Precondição", "Pré-condição");
            String passos = extrairCampoMultiplo(texto, "Passos", "Script de Teste \\(Passo-a-Passo\\)");
            String resultadoEsperado = extrairCampoMultiplo(texto, "Resultado esperado", "Script de Teste \\(Passo-a-Passo\\) - Resultado");
            
            // Fallback: se nome contém tudo, tentar extrair do texto corrido
            if (nome.contains("Objetivo:") || nome.contains("Pré-condições:") || nome.contains("Passos:")) {
                objetivo = extrairTextoEntre(nome, "Objetivo:", "Pré-condições:", "Passos:");
                precondicao = extrairTextoEntre(nome, "Pré-condições:", "Passos:", "Resultado esperado:");
                passos = extrairTextoEntre(nome, "Passos:", "Resultado esperado:", "Tipo:", "Prioridade:");
                resultadoEsperado = extrairTextoEntre(nome, "Resultado esperado:", "Tipo:", "Prioridade:", "Tags:");
                // Limpar nome para conter apenas o nome real
                nome = extrairTextoAte(nome, "Objetivo:", "Pré-condições:");
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

        String proximosCampos = String.join("|", java.util.Arrays.copyOfRange(campos, indice + 1, campos.length));

        String regex = proximosCampos.isBlank()
                ? campo + ":\\s*([\\s\\S]*?)$"
                : campo + ":\\s*([\\s\\S]*?)(?=\\n(?:" + proximosCampos + "):|$)";

        var matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(bloco);

        return matcher.find() ? matcher.group(1).trim() : "";
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

    private String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}