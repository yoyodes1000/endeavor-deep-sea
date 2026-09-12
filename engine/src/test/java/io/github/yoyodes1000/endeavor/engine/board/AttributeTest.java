package io.github.yoyodes1000.endeavor.engine.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AttributeTest {

    @Test
    void chaqueAttributSeRetrouveParSonCode() {
        for (Attribute attribute : Attribute.values()) {
            assertEquals(attribute, Attribute.fromCode(attribute.code()));
        }
    }

    @Test
    void unGainSansPisteNEstPasUnAttribut() {
        // « research » est un gain, mais pas une piste d'attribut
        assertThrows(IllegalArgumentException.class, () -> Attribute.fromCode("research"));
    }
}
