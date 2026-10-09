package io.github.yoyodes1000.endeavor.app.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Pose les en-têtes de sécurité de base sur chaque réponse : le navigateur ne devine
 * pas le type de contenu, n'affiche pas l'application dans le cadre d'un autre site, et
 * n'envoie pas son adresse en référent.
 *
 * <p>La politique de sécurité du contenu (CSP) viendra avec l'écran de jeu : elle doit
 * être réglée sur ce que l'interface Angular charge réellement.
 */
@Component
class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        chain.doFilter(request, response);
    }
}
