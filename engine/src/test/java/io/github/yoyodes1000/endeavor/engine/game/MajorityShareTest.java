package io.github.yoyodes1000.endeavor.engine.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MajorityShareTest {

    private static int[] scores(int... effectifs) {
        return effectifs;
    }

    @Test
    void unMeneurSeulTouchePremiereTrancheEtLeSecondLaSienne() {
        int[] e = scores(5, 3, 1);
        assertEquals(4, MajorityShare.twoTiers(e, 0, 4, 2));
        assertEquals(2, MajorityShare.twoTiers(e, 1, 4, 2));
        assertEquals(0, MajorityShare.twoTiers(e, 2, 4, 2));
    }

    @Test
    void lesExAequoEnTeteTouchentChacunLaMoyenneDesDeuxTranches() {
        assertEquals(3, MajorityShare.twoTiers(scores(5, 5, 1), 0, 4, 2));
        assertEquals(3, MajorityShare.twoTiers(scores(5, 5, 1), 1, 4, 2));
        assertEquals(0, MajorityShare.twoTiers(scores(5, 5, 1), 2, 4, 2), "pas de 2e quand deux joueurs sont en tête");
    }

    @Test
    void troisExAequoEnTeteTouchentChacunLaMemeMoyenne() {
        int[] e = scores(4, 4, 4);
        for (int player = 0; player < 3; player++) {
            assertEquals(3, MajorityShare.twoTiers(e, player, 4, 2));
        }
    }

    @Test
    void lesExAequoPourLaDeuxiemePlaceTouchentChacunLaTrancheEntiere() {
        int[] e = scores(6, 2, 2);
        assertEquals(4, MajorityShare.twoTiers(e, 0, 4, 2));
        assertEquals(2, MajorityShare.twoTiers(e, 1, 4, 2));
        assertEquals(2, MajorityShare.twoTiers(e, 2, 4, 2));
    }

    @Test
    void uneMoyenneArrondieAInferieur() {
        assertEquals(4, MajorityShare.twoTiers(scores(3, 3), 0, 5, 4));
    }

    @Test
    void unEffectifNulNeTouchePasMemeSiTousSontAZero() {
        assertEquals(0, MajorityShare.twoTiers(scores(0, 0, 0), 0, 4, 2));
        assertEquals(0, MajorityShare.leader(scores(0, 0), 1, 2));
        assertEquals(0, MajorityShare.twoTiers(scores(3, 0), 1, 4, 2), "le second à zéro ne touche rien");
    }

    @Test
    void lesLeadersExAequoTouchentChacunLeBonusEntier() {
        int[] e = scores(3, 3, 3, 1);
        assertEquals(2, MajorityShare.leader(e, 0, 2));
        assertEquals(2, MajorityShare.leader(e, 2, 2));
        assertEquals(0, MajorityShare.leader(e, 3, 2));
    }
}
