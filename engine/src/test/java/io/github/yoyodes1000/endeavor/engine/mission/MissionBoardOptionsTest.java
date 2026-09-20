package io.github.yoyodes1000.endeavor.engine.mission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.yoyodes1000.endeavor.engine.journal.FieldSymbol;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** Le choix d'option d'un objectif : posé en occupant l'hexagone objectif. */
class MissionBoardOptionsTest {

    private static MissionGoal.Standard standard(int number) {
        return new MissionGoal.Standard(number, List.of(GoalUnit.DISC), List.of(), List.of(), List.of(), 1,
                Optional.empty(), "t");
    }

    private static final MissionGoal.Choice CHOICE = new MissionGoal.Choice(2, List.of(
            new GoalOption("shallow", standard(2)), new GoalOption("deep", standard(2))));

    private static ImpactHex hex(int col, OptionalInt goal) {
        return new ImpactHex(0, col, 0, List.of(), true, false, false, Optional.empty(), false, 0, goal);
    }

    private static MissionBoard board(List<MissionGoal> goals) {
        return new MissionBoard(new ImpactBoard(HexOrientation.POINTY_TOP, List.of(
                hex(0, OptionalInt.of(2)), hex(1, OptionalInt.empty()), hex(2, OptionalInt.of(3)))), goals);
    }

    @Test
    void poserSurUnHexagoneObjectifOuvreLeChoixDeSonOption() {
        MissionBoard board = board(List.of(standard(1), CHOICE, standard(3)));
        assertEquals(OptionalInt.empty(), board.pendingOptionChoice());

        board.place(board.board().hexAt(0, 0).orElseThrow(), 1);

        assertEquals(OptionalInt.of(2), board.pendingOptionChoice());
        assertEquals(List.of("shallow", "deep"), board.pendingOptions().stream().map(GoalOption::id).toList());
        assertEquals(Optional.empty(), board.chosenOption(2), "pas encore choisi");
    }

    @Test
    void choisirRetientLOptionEtLeveLAttente() {
        MissionBoard board = board(List.of(CHOICE));
        board.place(board.board().hexAt(0, 0).orElseThrow(), 0);

        board.chooseOption("deep");

        assertEquals(OptionalInt.empty(), board.pendingOptionChoice());
        assertEquals("deep", board.chosenOption(2).orElseThrow().id());
    }

    @Test
    void unHexagoneOrdinaireOuSansObjectifAOptionsNOuvreAucunChoix() {
        MissionBoard board = board(List.of(standard(1), CHOICE, standard(3)));
        board.place(board.board().hexAt(0, 1).orElseThrow(), 0);
        board.place(board.board().hexAt(0, 2).orElseThrow(), 0); // objectif 3 : pas à options

        assertEquals(OptionalInt.empty(), board.pendingOptionChoice());
    }

    @Test
    void refuseUneOptionInconnueEtUnChoixSansAttente() {
        MissionBoard board = board(List.of(CHOICE));
        assertThrows(IllegalStateException.class, () -> board.chooseOption("shallow"));

        board.place(board.board().hexAt(0, 0).orElseThrow(), 0);
        assertThrows(IllegalArgumentException.class, () -> board.chooseOption("medium"));
        assertEquals(OptionalInt.of(2), board.pendingOptionChoice(), "l'attente reste après un choix refusé");
    }

    @Test
    void lOptionChoisieEtLAttenteSurviventALaCopie() {
        MissionBoard board = board(List.of(CHOICE));
        board.place(board.board().hexAt(0, 0).orElseThrow(), 0);
        MissionBoard pending = board.copy();
        board.chooseOption("shallow");
        MissionBoard chosen = board.copy();

        assertEquals(OptionalInt.of(2), pending.pendingOptionChoice(), "la copie garde l'attente");
        assertEquals("shallow", chosen.chosenOption(2).orElseThrow().id());
        assertTrue(pending.chosenOption(2).isEmpty(), "la copie d'avant le choix reste sans option");
    }

    @Test
    void unObjectifAOptionsExigeAuMoinsDeuxOptionsDistinctes() {
        GoalOption only = new GoalOption("a", standard(2));
        assertThrows(IllegalArgumentException.class, () -> new MissionGoal.Choice(2, List.of(only)));
        assertThrows(IllegalArgumentException.class, () -> new MissionGoal.Choice(2, List.of(only, only)));
    }

    @Test
    void unSymboleCouleurNEstPasUnObjectif() {
        ImpactHex plain = new ImpactHex(0, 0, 0, List.of(), false, false, false,
                Optional.of(FieldSymbol.BLUE), false, 1);
        assertEquals(OptionalInt.empty(), plain.goal());
    }
}
