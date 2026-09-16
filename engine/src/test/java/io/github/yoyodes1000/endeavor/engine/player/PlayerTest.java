package io.github.yoyodes1000.endeavor.engine.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFace;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void auDepartLeJoueurNADetientQueLeChefDEquipe() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);

        assertEquals(10, player.reserveDiscs());
        assertEquals(0, player.transitDiscs());
        assertEquals(1, player.specialists().size());
        assertTrue(player.specialists().get(0).specialist().teamLeader());
        assertEquals(SpecialistFace.JUNIOR, player.specialists().get(0).face());
        assertEquals(1, player.attributes().level(Attribute.INSPIRATION));
    }

    @Test
    void refuseDeDemarrerSansChefDEquipe() {
        assertThrows(IllegalArgumentException.class,
                () -> Player.start(PlayerFixtures.ranked("pilot"), 10));
    }

    @Test
    void lEffortDeplaceDesDisquesDeLaReserveVersLeTransit() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.moveReserveToTransit(3);

        assertEquals(7, player.reserveDiscs());
        assertEquals(3, player.transitDiscs());
    }

    @Test
    void lEffortRefuseDePrendrePlusQueLaReserve() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 2);
        assertThrows(IllegalArgumentException.class, () -> player.moveReserveToTransit(3));
    }

    @Test
    void laListeDesTuilesEstEnLectureSeule() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertThrows(UnsupportedOperationException.class,
                () -> player.specialists().add(HeldSpecialist.recruited(PlayerFixtures.ranked("x"))));
    }

    @Test
    void laRecuperationRepasseUnDisqueDeLaTuileVersLeTransit() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.recruit(new HeldSpecialist(PlayerFixtures.ranked("pilot"), SpecialistFace.JUNIOR, 2));

        assertTrue(player.hasRecoverableDisc());
        player.recoverDisc("pilot");

        assertEquals(1, player.transitDiscs(), "le disque repris arrive en transit");
        assertEquals(1, player.specialists().get(1).placedDiscs(), "un disque de moins sur la tuile");
    }

    @Test
    void laRecuperationRefuseUneTuileSansDisque() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertFalse(player.hasRecoverableDisc());
        assertThrows(IllegalArgumentException.class, () -> player.recoverDisc("team-leader"));
    }

    @Test
    void laRecuperationRefuseUneTuileNonDetenue() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertThrows(IllegalArgumentException.class, () -> player.recoverDisc("inconnu"));
    }

    @Test
    void lActivationPoseUnDisqueDeTransitSurLaCaseLibre() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.moveReserveToTransit(2);

        player.activate("team-leader");

        assertEquals(1, player.transitDiscs(), "un disque de transit consommé");
        assertEquals(1, player.specialists().get(0).placedDiscs(), "la case d'activation est occupée");
    }

    @Test
    void lActivationRefuseUneCaseDejaOccupee() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.moveReserveToTransit(2);
        player.activate("team-leader");

        assertThrows(IllegalArgumentException.class, () -> player.activate("team-leader"));
    }

    @Test
    void lActivationRefuseSansDisqueEnTransit() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertThrows(IllegalArgumentException.class, () -> player.activate("team-leader"));
    }

    @Test
    void lActivationRefuseUneTuileNonDetenue() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.moveReserveToTransit(1);
        assertThrows(IllegalArgumentException.class, () -> player.activate("inconnu"));
    }

    @Test
    void auDepartLaReserveDeSubmersiblesEstVide() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertEquals(0, player.vesselStock());
    }

    @Test
    void leGainDeSubmersiblesAlimenteLaReserve() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.gainVessels(2);
        assertEquals(2, player.vesselStock());
    }

    @Test
    void sortirUnSubmersibleDeLaReserveLaDiminue() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.gainVessels(2);
        player.takeVesselFromStock();
        assertEquals(1, player.vesselStock());
    }

    @Test
    void onNePeutPasSortirUnSubmersibleDUneReserveVide() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertThrows(IllegalArgumentException.class, player::takeVesselFromStock);
    }

    @Test
    void laCopieEstIndependante() {
        Player original = Player.start(PlayerFixtures.teamLeader(), 10);

        Player copie = original.copy();
        copie.moveReserveToTransit(4);
        copie.attributes().advance(Attribute.COORDINATION, 2);
        copie.gainVessels(2);
        copie.receiveDiveToken("research");

        assertEquals(10, original.reserveDiscs(), "les disques de l'original ne bougent pas");
        assertEquals(0, original.attributes().step(Attribute.COORDINATION), "les pistes de l'original ne bougent pas");
        assertEquals(0, original.vesselStock(), "la réserve de submersibles de l'original ne bouge pas");
        assertTrue(original.heldDiveTokens().isEmpty(), "la main de jetons de l'original ne bouge pas");
        assertEquals(6, copie.reserveDiscs());
        assertEquals(2, copie.attributes().step(Attribute.COORDINATION));
        assertEquals(2, copie.vesselStock());
        assertEquals(List.of("research"), copie.heldDiveTokens());
    }

    // --- Jetons de plongée ---------------------------------------------------

    @Test
    void unJetonRecuRejointLaMain() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.receiveDiveToken("research");
        player.receiveDiveToken("sonar");

        assertEquals(List.of("research", "sonar"), player.heldDiveTokens());
    }

    @Test
    void resoudreUnJetonLeRetireDeLaMain() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.receiveDiveToken("research");
        player.receiveDiveToken("sonar");

        assertEquals("research", player.resolveDiveToken(0));

        assertEquals(List.of("sonar"), player.heldDiveTokens());
    }

    @Test
    void laMainDeJetonsEstEnLectureSeule() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        assertThrows(UnsupportedOperationException.class, () -> player.heldDiveTokens().add("research"));
    }

    @Test
    void depenserUnDisqueDeReserveLeDiminue() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.spendReserveDisc();
        assertEquals(9, player.reserveDiscs());
    }

    @Test
    void onNePeutPasDepenserUnDisqueDeReserveVide() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 0);
        assertThrows(IllegalArgumentException.class, player::spendReserveDisc);
    }

    @Test
    void depenserDeLaRechercheLaDiminue() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.gainResearch(5);
        player.spendResearch(3);
        assertEquals(2, player.research());
    }

    @Test
    void onNePeutPasDepenserPlusDeRechercheQueDisponible() {
        Player player = Player.start(PlayerFixtures.teamLeader(), 10);
        player.gainResearch(2);
        assertThrows(IllegalArgumentException.class, () -> player.spendResearch(3));
    }
}
