package io.github.yoyodes1000.endeavor.engine.board;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AttributesTest {

    @Test
    void auDepartTouteLesPistesSontAuNiveauUn() {
        Attributes attributes = Attributes.atStart();
        for (Attribute attribute : Attribute.values()) {
            assertEquals(0, attributes.step(attribute));
            assertEquals(1, attributes.level(attribute));
        }
        assertEquals(0, attributes.totalPoints());
    }

    @Test
    void avancerNeToucheQueLaPisteVisee() {
        Attributes attributes = Attributes.atStart();
        attributes.advance(Attribute.INSPIRATION, 4);

        assertEquals(4, attributes.step(Attribute.INSPIRATION));
        assertEquals(3, attributes.level(Attribute.INSPIRATION));
        assertEquals(0, attributes.step(Attribute.COORDINATION));
        assertEquals(1, attributes.level(Attribute.COORDINATION));
    }

    @Test
    void leTotalDesPointsSommeLesPaliers() {
        Attributes attributes = Attributes.atStart();
        attributes.advance(Attribute.REPUTATION, 2);   // niveau 2 -> 1 point
        attributes.advance(Attribute.INGENUITY, 10);   // niveau 5 -> 10 points
        assertEquals(11, attributes.totalPoints());
    }

    @Test
    void laCopieEstIndependante() {
        Attributes original = Attributes.atStart();
        original.advance(Attribute.COORDINATION, 3);

        Attributes copie = original.copy();
        copie.advance(Attribute.COORDINATION, 3);

        assertEquals(3, original.step(Attribute.COORDINATION), "l'original ne bouge pas");
        assertEquals(6, copie.step(Attribute.COORDINATION), "la copie évolue seule");
    }
}
