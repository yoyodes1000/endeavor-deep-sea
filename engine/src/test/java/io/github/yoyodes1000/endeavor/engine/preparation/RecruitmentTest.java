package io.github.yoyodes1000.endeavor.engine.preparation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.RandomSource;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.effect.EffectOutcome;
import io.github.yoyodes1000.endeavor.engine.game.GameState;
import io.github.yoyodes1000.endeavor.engine.player.Player;
import io.github.yoyodes1000.endeavor.engine.specialist.Gain;
import io.github.yoyodes1000.endeavor.engine.specialist.Specialist;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistRoster;
import io.github.yoyodes1000.endeavor.engine.specialist.SpecialistSide;
import io.github.yoyodes1000.endeavor.engine.support.Fixtures;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class RecruitmentTest {

    private static GameState game() {
        return GameState.newGame(2, Fixtures.roster(), RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());
    }

    private static List<String> recruitableIds(GameState state, int playerIndex) {
        return Recruitment.legalRecruits(state, playerIndex).stream().map(Recruter::specialistId).toList();
    }

    @Test
    void auNiveauUnSeulesLesTuilesDeRangUnSontRecrutables() {
        GameState state = game(); // réputation au niveau 1 au départ
        assertEquals(List.of("pilot"), recruitableIds(state, 0));
    }

    @Test
    void uneReputationPlusHauteOuvrePlusDeTuiles() {
        GameState state = game();
        state.player(0).attributes().advance(Attribute.REPUTATION, 2); // niveau 2
        assertEquals(List.of("pilot", "ecologist", "navigator"), recruitableIds(state, 0));
    }

    @Test
    void recruterDeplaceLaTuileDuCasierVersLeJoueur() {
        GameState state = game();
        Recruitment.applyRecruit(state, 0, new Recruter("pilot"));

        Player player = state.player(0);
        assertEquals(2, player.specialists().size()); // chef d'équipe + pilot
        assertEquals("pilot", player.specialists().get(1).specialist().id());
        assertEquals(2, state.casier().size());
        assertTrue(state.casier().stream().noneMatch(specialist -> specialist.id().equals("pilot")));
    }

    @Test
    void refuseUnRecrutementAuDessusDeLaReputation() {
        GameState state = game(); // réputation 1 -> ecologist (rang 2) illégal
        assertThrows(IllegalArgumentException.class,
                () -> Recruitment.applyRecruit(state, 0, new Recruter("ecologist")));
    }

    @Test
    void refuseUneTuileAbsenteDuCasier() {
        GameState state = game();
        assertThrows(IllegalArgumentException.class,
                () -> Recruitment.applyRecruit(state, 0, new Recruter("inconnu")));
    }

    @Test
    void unRecrutementViseUnIdentifiantNonVide() {
        assertThrows(IllegalArgumentException.class, () -> new Recruter("  "));
    }

    @Test
    void recruterResoutLesGainsDeLaTuile() {
        // un rang 1 qui donne un impact à l'arrivée
        SpecialistSide junior = new SpecialistSide("Giver", List.of(Gain.IMPACT), List.of(),
                Optional.empty(), Optional.empty());
        SpecialistSide senior = new SpecialistSide("Giver S", List.of(), List.of(),
                Optional.empty(), Optional.empty());
        Specialist giver = new Specialist("giver", OptionalInt.of(1), false, junior, senior);
        SpecialistRoster roster = new SpecialistRoster(List.of(Fixtures.teamLeader(), giver));
        GameState state = GameState.newGame(1, roster, RandomSource.fromSeed(1), 10,
                Fixtures.missionBoard(), Fixtures.oceanBoard(), Fixtures.oceanCatalog(), Fixtures.diveTokenCatalog(),
                Fixtures.journalCatalog());

        EffectOutcome outcome = Recruitment.applyRecruit(state, 0, new Recruter("giver"));

        assertEquals(1, outcome.impactsEarned(), "le gain impact de la tuile est remonté");
    }
}
