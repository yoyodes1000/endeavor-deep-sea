package io.github.yoyodes1000.endeavor.app.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import io.github.yoyodes1000.endeavor.engine.action.Activer;
import io.github.yoyodes1000.endeavor.engine.action.ChoisirAttribut;
import io.github.yoyodes1000.endeavor.engine.action.ChoisirOption;
import io.github.yoyodes1000.endeavor.engine.action.Conserver;
import io.github.yoyodes1000.endeavor.engine.action.DepenserJeton;
import io.github.yoyodes1000.endeavor.engine.action.Dive;
import io.github.yoyodes1000.endeavor.engine.action.GarderTuile;
import io.github.yoyodes1000.endeavor.engine.action.Passer;
import io.github.yoyodes1000.endeavor.engine.action.PoserImpact;
import io.github.yoyodes1000.endeavor.engine.action.PoserTuile;
import io.github.yoyodes1000.endeavor.engine.action.Promouvoir;
import io.github.yoyodes1000.endeavor.engine.action.Publier;
import io.github.yoyodes1000.endeavor.engine.action.Recruter;
import io.github.yoyodes1000.endeavor.engine.action.Recuperer;
import io.github.yoyodes1000.endeavor.engine.action.Sonar;
import io.github.yoyodes1000.endeavor.engine.action.TerminerTour;
import io.github.yoyodes1000.endeavor.engine.action.Voyager;
import io.github.yoyodes1000.endeavor.engine.board.Attribute;
import io.github.yoyodes1000.endeavor.engine.ocean.Cell;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ActionJsonModuleTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().addModule(new ActionJsonModule()).build();

    private static final Cell CELL = new Cell(1, 2);

    /** Un exemple de chaque coup du moteur. */
    private static final List<Action> SAMPLES = List.of(
            new Recruter("pilot"), new PoserImpact(0, 1), new Recuperer("pilot"), new Passer(),
            new Activer("pilot"), new TerminerTour(), new Voyager(CELL, new Cell(2, 2)), new Sonar(CELL, 1),
            new GarderTuile("shipwreck"), new PoserTuile(CELL), new Dive(CELL, "d1"), new DepenserJeton(0, 1),
            new Conserver(CELL, "c1"), new Publier("journal", CELL, "p1"), new Promouvoir("pilot"),
            new ChoisirAttribut(Attribute.INGENUITY), new ChoisirOption("option-a"));

    private static Action roundTrip(Action action) throws JsonProcessingException {
        return MAPPER.readValue(MAPPER.writeValueAsString(action), Action.class);
    }

    @Test
    void lesExemplesCouvrentChaqueCoupDuMoteur() {
        Set<Class<?>> sampled = SAMPLES.stream().map(Object::getClass).collect(Collectors.toSet());
        assertEquals(Set.copyOf(Arrays.asList(Action.class.getPermittedSubclasses())), sampled,
                "un coup ajouté au moteur doit avoir son exemple ici");
    }

    @Test
    void chaqueCoupFaitLAllerRetourEnJson() throws JsonProcessingException {
        for (Action action : SAMPLES) {
            assertEquals(action, roundTrip(action), action.getClass().getSimpleName());
        }
    }

    @Test
    void unCoupPorteSonTypeASesParametres() throws JsonProcessingException {
        assertEquals("{\"type\":\"Voyager\",\"from\":{\"depth\":1,\"col\":2},\"to\":{\"depth\":2,\"col\":2}}",
                MAPPER.writeValueAsString(new Voyager(CELL, new Cell(2, 2))));
    }

    @Test
    void unCoupSansParametreSeReduitASonType() throws JsonProcessingException {
        assertEquals("{\"type\":\"Passer\"}", MAPPER.writeValueAsString(new Passer()));
        assertEquals(new Passer(), MAPPER.readValue("{\"type\":\"Passer\"}", Action.class));
    }

    @Test
    void unTypeInconnuEstRefuse() {
        assertThrows(JsonProcessingException.class, () -> MAPPER.readValue("{\"type\":\"Tricher\"}", Action.class));
    }

    @Test
    void unCoupSansTypeEstRefuse() {
        assertThrows(JsonProcessingException.class,
                () -> MAPPER.readValue("{\"specialistId\":\"pilot\"}", Action.class));
    }

    @Test
    void desParametresInvalidesSontRefusesParLeMoteur() {
        assertThrows(JsonProcessingException.class,
                () -> MAPPER.readValue("{\"type\":\"PoserTuile\",\"cell\":{\"depth\":0,\"col\":0}}", Action.class));
    }
}
