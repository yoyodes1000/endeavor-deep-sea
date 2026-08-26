package io.github.yoyodes1000.endeavor.app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Point de contrôle minimal, qui permet de vérifier que l'application répond et
 * que l'interface parvient bien à joindre l'API.
 */
@RestController
@RequestMapping("/api")
class VersionController {

    private final String version;

    VersionController(@Value("${application.version}") String version) {
        this.version = version;
    }

    @GetMapping("/version")
    Version version() {
        return new Version("Endeavor : Eaux profondes", version);
    }

    record Version(String application, String version) {
    }
}
