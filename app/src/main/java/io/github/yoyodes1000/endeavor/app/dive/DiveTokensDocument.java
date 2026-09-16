package io.github.yoyodes1000.endeavor.app.dive;

import java.util.List;

/**
 * Reflet brut de {@code dive-tokens.json} : chaque type de jeton, son nombre
 * d'exemplaires, et ses options — un lot de gains (avec coût optionnel) ou une
 * action accordée, jamais les deux dans la même option.
 */
record DiveTokensDocument(List<Entry> diveTokens) {

    record Entry(String id, Integer copies, List<Option> options) {
    }

    /** Une option : {@code gains}/{@code cost} <strong>ou</strong> {@code action}, jamais les deux. */
    record Option(List<String> gains, List<String> cost, ActionRef action) {
    }

    /** L'action accordée par une option, avec son modificateur de coût optionnel. */
    record ActionRef(String type, Integer costModifier) {
    }
}
