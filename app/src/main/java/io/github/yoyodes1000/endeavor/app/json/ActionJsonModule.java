package io.github.yoyodes1000.endeavor.app.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.yoyodes1000.endeavor.engine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Le format JSON des coups : un objet qui porte son type — le nom du record du
 * moteur — à côté de ses paramètres, par exemple
 * {@code {"type":"Voyager","from":{"depth":1,"col":2},"to":{"depth":2,"col":2}}}.
 *
 * <p>Les types sont lus dans la liste scellée de {@link Action} : un coup ajouté au
 * moteur se sérialise sans rien toucher ici. Le polymorphisme est déclaré par un
 * mix-in, si bien que le moteur reste ignorant de Jackson.
 *
 * <p>Déclaré comme composant, ce module est enregistré par Spring Boot dans
 * l'{@code ObjectMapper} de l'application.
 */
@Component
public class ActionJsonModule extends SimpleModule {

    public static final String TYPE_PROPERTY = "type";

    public ActionJsonModule() {
        super("endeavor-actions");
        setMixInAnnotation(Action.class, ActionMixIn.class);
        for (Class<?> actionType : Action.class.getPermittedSubclasses()) {
            registerSubtypes(new NamedType(actionType, actionType.getSimpleName()));
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = TYPE_PROPERTY)
    private interface ActionMixIn {
    }
}
