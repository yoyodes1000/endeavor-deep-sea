package io.github.yoyodes1000.endeavor.engine.mission;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** L'ensemble des missions du matériel. Numéros uniques garantis. */
public record MissionCatalog(List<Mission> missions) {

    public MissionCatalog {
        if (missions == null || missions.isEmpty()) {
            throw new IllegalArgumentException("Le catalogue de missions est vide");
        }
        missions = List.copyOf(missions);

        Set<Integer> numbers = new HashSet<>();
        for (Mission mission : missions) {
            if (!numbers.add(mission.number())) {
                throw new IllegalArgumentException("Numéro de mission en double : " + mission.number());
            }
        }
    }

    /** La mission portant ce numéro, si elle existe. */
    public Optional<Mission> byNumber(int number) {
        return missions.stream().filter(mission -> mission.number() == number).findFirst();
    }
}
