package io.github.yoyodes1000.endeavor.engine.journal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import java.util.List;
import org.junit.jupiter.api.Test;

class JournalTest {

    private static Journal journal(String id, List<FieldSymbol> fieldSymbols) {
        return new Journal(id, false, "Nom", 2, 1, fieldSymbols, List.of(Gain.RESEARCH), List.of());
    }

    @Test
    void accepteUneRevueValide() {
        assertDoesNotThrow(() -> journal("j", List.of(FieldSymbol.BLUE)));
    }

    @Test
    void refuseUnIdentifiantVide() {
        assertThrows(IllegalArgumentException.class, () -> journal(" ", List.of(FieldSymbol.BLUE)));
    }

    @Test
    void refuseUnCoutDeRechercheNegatif() {
        assertThrows(IllegalArgumentException.class,
                () -> new Journal("j", false, "Nom", -1, 0, List.of(FieldSymbol.BLUE), List.of(), List.of()));
    }

    @Test
    void refuseDesPointsDeVictoireNegatifs() {
        assertThrows(IllegalArgumentException.class,
                () -> new Journal("j", false, "Nom", 0, -1, List.of(FieldSymbol.BLUE), List.of(), List.of()));
    }

    @Test
    void refuseUneRevueSansSymboleDeDomaine() {
        assertThrows(IllegalArgumentException.class, () -> journal("j", List.of()));
    }

    @Test
    void lesListesSontImmuables() {
        Journal journal = journal("j", List.of(FieldSymbol.BLUE));
        assertThrows(UnsupportedOperationException.class, () -> journal.fieldSymbols().add(FieldSymbol.GREEN));
        assertThrows(UnsupportedOperationException.class, () -> journal.publisherGains().add(Gain.DISC));
        assertThrows(UnsupportedOperationException.class, () -> journal.opponentsGains().add(Gain.DISC));
    }
}
