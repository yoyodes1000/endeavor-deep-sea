package io.github.yoyodes1000.endeavor.app.material;

import io.github.yoyodes1000.endeavor.app.dive.DiveTokenLoader;
import io.github.yoyodes1000.endeavor.app.journal.JournalLoader;
import io.github.yoyodes1000.endeavor.app.mission.MissionLoader;
import io.github.yoyodes1000.endeavor.app.ocean.OceanTileLoader;
import io.github.yoyodes1000.endeavor.app.specialist.SpecialistLoader;
import io.github.yoyodes1000.endeavor.engine.mission.MissionCatalog;
import io.github.yoyodes1000.endeavor.engine.play.GameSetup;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * Charge le matériel du jeu une fois pour toutes, au démarrage, depuis les fichiers
 * {@code data/*.json} embarqués dans le jar.
 *
 * <p>Les chargeurs valident strictement : une description invalide empêche
 * l'application de démarrer plutôt que de dégrader une partie en silence (décision 4
 * de l'architecture).
 */
@Configuration
public class MaterialConfiguration {

    private static final String DATA_DIRECTORY = "data/";

    @Bean
    public MissionCatalog missionCatalog() {
        return load("missions.json", new MissionLoader()::load);
    }

    @Bean
    public GameSetup.Materials materials() {
        return new GameSetup.Materials(
                load("specialists.json", new SpecialistLoader()::load),
                load("ocean-tiles.json", new OceanTileLoader()::load),
                load("dive-tokens.json", new DiveTokenLoader()::load),
                load("journals.json", new JournalLoader()::load));
    }

    private static <T> T load(String file, Function<Reader, T> loader) {
        ClassPathResource resource = new ClassPathResource(DATA_DIRECTORY + file);
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return loader.apply(reader);
        } catch (IOException unreadable) {
            throw new UncheckedIOException("Matériel illisible : " + DATA_DIRECTORY + file, unreadable);
        }
    }
}
