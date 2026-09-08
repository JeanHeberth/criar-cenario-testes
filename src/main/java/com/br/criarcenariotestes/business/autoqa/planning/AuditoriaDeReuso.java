package com.br.criarcenariotestes.business.autoqa.planning;

import com.br.criarcenariotestes.business.autoqa.model.knowledge.ProjectComponent;
import com.br.criarcenariotestes.business.autoqa.model.planning.FileOperation;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlannedFileAction;
import com.br.criarcenariotestes.business.autoqa.model.planning.PlanningWarning;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Questiona cada decisão de CRIAR um arquivo, cruzando com o que o
 * PROJECT_KNOWLEDGE já encontrou no projeto.
 *
 * <p>O pipeline reutiliza arquivos, mas nunca auditava a decisão de criar: não
 * havia registro de POR QUE criar era necessário, e duplicação passava
 * silenciosa. Um plano podia criar um cliente ao lado de outro equivalente sem
 * que nada acusasse.
 *
 * <p>Deliberadamente NÃO altera o plano. Trocar CREATE por UPDATE por conta
 * própria significaria modificar arquivo existente do usuário sem que ele
 * tivesse pedido — decisão dele, não do sistema. A auditoria informa; quem
 * decide é quem lê.
 */
@Component
public class AuditoriaDeReuso {

    public enum Tipo {
        /** Existe equivalente em OUTRO caminho: provável duplicação. */
        ESTENDER,
        /** Sem correspondente: criação justificada. */
        CRIAR
    }

    public record Veredito(String relativePath, Tipo tipo, String justificativa, String localizacao) {}

    public List<Veredito> auditar(List<PlannedFileAction> acoes, List<ProjectComponent> componentes) {
        if (acoes == null || acoes.isEmpty()) {
            return List.of();
        }
        List<ProjectComponent> existentes = componentes == null ? List.of() : componentes;

        List<Veredito> vereditos = new ArrayList<>();
        for (PlannedFileAction acao : acoes) {
            if (acao == null || acao.operation() != FileOperation.CREATE || acao.relativePath() == null) {
                continue;
            }
            vereditos.add(auditarCriacao(acao, existentes));
        }
        return List.copyOf(vereditos);
    }

    private Veredito auditarCriacao(PlannedFileAction acao, List<ProjectComponent> existentes) {
        String caminho = acao.relativePath();

        // CREATE sobre caminho existente NÃO é tratado aqui: o próprio
        // PlanningValidator já reprova esse caso, e de forma mais estrita —
        // lança exceção em vez de advertir. Duplicar a regra aqui só produziria
        // um aviso que ninguém chega a ler.
        String nomeDoArquivo = nomeDeArquivo(caminho);
        ProjectComponent mesmoNome = existentes.stream()
                .filter(Objects::nonNull)
                .filter(c -> c.relativePath() != null)
                // O próprio caminho fica de fora: mesmo caminho é o caso já
                // coberto pela validação, e compará-lo por nome o classificaria
                // como duplicação de si mesmo.
                .filter(c -> !caminho.equals(c.relativePath()))
                .filter(c -> nomeDoArquivo.equals(nomeDeArquivo(c.relativePath())))
                .findFirst()
                .orElse(null);
        if (mesmoNome != null) {
            return new Veredito(caminho, Tipo.ESTENDER,
                    "Existe componente de mesmo nome em outro caminho — provável duplicação",
                    mesmoNome.relativePath());
        }

        return new Veredito(caminho, Tipo.CRIAR, "Nenhum componente existente cobre este artefato", null);
    }

    private String nomeDeArquivo(String caminho) {
        int barra = caminho.lastIndexOf('/');
        return (barra >= 0 ? caminho.substring(barra + 1) : caminho).toLowerCase(Locale.ROOT);
    }

    /**
     * Converte os vereditos em advertências do plano — só assim o resultado
     * chega a quem decide, em vez de ficar num log.
     *
     * <p>CRIAR não vira advertência: criação justificada é o caso normal e
     * transformá-la em aviso encheria o plano de ruído, o que faz o leitor
     * parar de ler os avisos que importam.
     *
     * <p>ESTENDER pede decisão humana: só quem conhece o domínio sabe se dois
     * arquivos de mesmo nome são duplicação ou separação legítima por contexto.
     */
    public List<PlanningWarning> comoAdvertencias(List<Veredito> vereditos) {
        if (vereditos == null || vereditos.isEmpty()) {
            return List.of();
        }
        return vereditos.stream()
                .filter(v -> v != null && v.tipo() != Tipo.CRIAR)
                .map(v -> new PlanningWarning("POSSIVEL_DUPLICACAO", descrever(v), true))
                .toList();
    }

    private String descrever(Veredito veredito) {
        StringBuilder texto = new StringBuilder()
                .append(veredito.relativePath())
                .append(" planejado como CREATE, mas a auditoria indica ")
                .append(veredito.tipo())
                .append(": ")
                .append(veredito.justificativa());
        if (veredito.localizacao() != null) {
            texto.append(". Existente em: ").append(veredito.localizacao());
        }
        return texto.toString();
    }
}
