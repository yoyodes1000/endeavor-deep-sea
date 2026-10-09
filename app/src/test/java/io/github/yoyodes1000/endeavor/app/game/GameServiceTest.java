package io.github.yoyodes1000.endeavor.app.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.app.material.MaterialConfiguration;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameServiceTest {

    private static final MaterialConfiguration MATERIAL = new MaterialConfiguration();

    private static GameService newService() {
        return new GameService(MATERIAL.missionCatalog(), MATERIAL.materials());
    }

    @Test
    void lesMissionsJouablesSontCellesQuiOntUneZoneDeLancement() {
        List<MissionSummary> missions = newService().playableMissions();

        assertEquals(List.of(1, 2, 4, 7, 8, 9, 10), missions.stream().map(MissionSummary::number).toList());
        assertTrue(missions.get(0).allGoalsScored(), "la mission 1 est entièrement comptée");
    }

    @Test
    void sansPartieLanceeIlNyARienALireNiAJouer() {
        GameService service = newService();

        assertThrows(NoGameInProgressException.class, service::view);
        assertThrows(NoGameInProgressException.class, service::result);
        assertThrows(NoGameInProgressException.class, () -> service.play(new MoveRequest(0, new Passer())));
    }

    @Test
    void uneDemandeDePartieIncompleteOuHorsBornesEstRefusee() {
        GameService service = newService();

        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(null, 2, null)));
        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(99, 2, null)));
        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(3, 2, null)),
                "la mission 3 n'a pas encore de zone de lancement");
        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(1, null, null)));
        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(1, 0, null)));
        assertThrows(InvalidGameRequestException.class, () -> service.start(new NewGameRequest(1, 4, null)));
    }

    @Test
    void unCoupSansNumeroOuSansActionEstRefuse() {
        GameService service = newService();
        service.start(new NewGameRequest(1, 1, 1L));

        assertThrows(InvalidGameRequestException.class, () -> service.play(new MoveRequest(null, new Passer())));
        assertThrows(InvalidGameRequestException.class, () -> service.play(new MoveRequest(0, null)));
    }

    @Test
    void uneNouvellePartieSeMetEnPlaceAvecSesAdversaires() {
        GameView view = newService().start(new NewGameRequest(2, 3, 11L));

        assertEquals(2, view.mission());
        assertEquals(4, view.playerCount());
        assertEquals(1, view.round());
        assertEquals(GameSession.HUMAN_SEAT, view.yourSeat());
        assertTrue(view.yourTurn());
    }

    @Test
    void aGraineEgaleLaPartieEtLesBotsRejouentLesMemesCoups() {
        GameService first = newService();
        GameService second = newService();
        GameView a = first.start(new NewGameRequest(1, 2, 42L));
        GameView b = second.start(new NewGameRequest(1, 2, 42L));

        for (int move = 0; move < 30 && a.yourTurn(); move++) {
            assertEquals(a, b);
            a = first.play(new MoveRequest(a.moveNumber(), a.legalActions().get(0)));
            b = second.play(new MoveRequest(b.moveNumber(), b.legalActions().get(0)));
        }
        assertEquals(a, b);
    }
}
