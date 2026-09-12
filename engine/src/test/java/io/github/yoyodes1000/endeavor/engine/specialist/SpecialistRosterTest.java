package io.github.yoyodes1000.endeavor.engine.specialist;

import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.ranked;
import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.rankFive;
import static io.github.yoyodes1000.endeavor.engine.specialist.SpecialistFixtures.teamLeader;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpecialistRosterTest {

    @Test
    void accepteUnCasierValide() {
        SpecialistRoster roster =
                new SpecialistRoster(List.of(teamLeader(), ranked("pilot", 1), rankFive("mentor")));
        assertEquals(3, roster.specialists().size());
    }

    @Test
    void refuseUnCasierVide() {
        assertThrows(IllegalArgumentException.class, () -> new SpecialistRoster(List.of()));
    }

    @Test
    void refuseUnIdentifiantEnDouble() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                new SpecialistRoster(List.of(teamLeader(), ranked("dup", 1), ranked("dup", 2))));
        assertTrue(erreur.getMessage().contains("double"));
    }

    @Test
    void refuseUnCasierSansChefDEquipe() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                new SpecialistRoster(List.of(ranked("a", 1), ranked("b", 2))));
        assertTrue(erreur.getMessage().contains("chef d'équipe"));
    }

    @Test
    void refuseDeuxChefsDEquipe() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class, () ->
                new SpecialistRoster(List.of(teamLeader("l1"), teamLeader("l2"))));
        assertTrue(erreur.getMessage().contains("chef d'équipe"));
    }

    @Test
    void retrouveParIdentifiantParRangEtLeChef() {
        Specialist chef = teamLeader();
        Specialist pilote = ranked("pilot", 1);
        Specialist mentor = rankFive("mentor");
        SpecialistRoster roster = new SpecialistRoster(List.of(chef, pilote, mentor));

        assertEquals(pilote, roster.byId("pilot").orElseThrow());
        assertTrue(roster.byId("absent").isEmpty());
        assertEquals(chef, roster.teamLeader());
        assertEquals(List.of(mentor), roster.ofRank(5));
        assertEquals(List.of(pilote), roster.ofRank(1));
        assertTrue(roster.ofRank(3).isEmpty());
    }
}
