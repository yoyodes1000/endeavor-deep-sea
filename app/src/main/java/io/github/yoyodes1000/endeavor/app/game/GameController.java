package io.github.yoyodes1000.endeavor.app.game;

import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * L'API de partie : lancer une partie, la lire, y jouer, en lire le décompte.
 *
 * <p>Les requêtes qui modifient la partie n'acceptent que du JSON. Un formulaire ou un
 * texte brut envoyé par une page d'un autre site est donc refusé, et une requête JSON
 * venue d'ailleurs exige une autorisation CORS que l'application ne donne jamais.
 */
@RestController
@RequestMapping("/api")
class GameController {

    private static final URI GAME = URI.create("/api/game");

    private final GameService games;

    GameController(GameService games) {
        this.games = games;
    }

    @GetMapping("/missions")
    List<MissionSummary> missions() {
        return games.playableMissions();
    }

    @PostMapping(path = "/game", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<GameView> start(@RequestBody NewGameRequest request) {
        return ResponseEntity.created(GAME).body(games.start(request));
    }

    @GetMapping("/game")
    GameView view() {
        return games.view();
    }

    @PostMapping(path = "/game/moves", consumes = MediaType.APPLICATION_JSON_VALUE)
    GameView play(@RequestBody MoveRequest request) {
        return games.play(request);
    }

    @GetMapping("/game/result")
    ResultView result() {
        return games.result();
    }
}
