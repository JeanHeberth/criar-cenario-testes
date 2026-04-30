package com.br.criarcenariotestes.business.parser;

import com.br.criarcenariotestes.infrastructure.entity.CenarioItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CenarioTextoParser {

    public List<CenarioItem> parsear(String resposta) {
        List<CenarioItem> itens = new ArrayList<>();

        if (resposta == null || resposta.isBlank()) {
            return itens;
        }

        String[] blocos = resposta.split("(?m)^---\\s*$");

        for (String bloco : blocos) {
            String texto = bloco.trim();

            if (!texto.contains("Nome:")) {
                continue;
            }

            CenarioItem item = CenarioItem.builder()
                    .nome(extrairCampo(texto, "Nome"))
                    .objetivo(extrairCampo(texto, "Objetivo"))
                    .precondicao(extrairCampo(texto, "Precondição"))
                    .scriptTeste(extrairCampo(texto, "Script de Teste \\(Passo-a-Passo\\)"))
                    .resultadoEsperado(extrairCampo(texto, "Script de Teste \\(Passo-a-Passo\\) - Resultado"))
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
                "Precondição",
                "Script de Teste \\(Passo-a-Passo\\)",
                "Script de Teste \\(Passo-a-Passo\\) - Resultado",
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

    private String valorOuPadrao(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}