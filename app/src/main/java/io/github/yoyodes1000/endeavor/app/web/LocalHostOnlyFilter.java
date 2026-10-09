package io.github.yoyodes1000.endeavor.app.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuse toute requête qui ne s'adresse pas à la machine locale par son nom.
 *
 * <p>Parade au « DNS rebinding » : une page malveillante fait pointer son propre nom
 * de domaine vers 127.0.0.1, puis pilote l'application depuis le navigateur de
 * l'utilisateur comme si elle était du même site. L'écoute sur la seule boucle locale
 * ({@code server.address}) n'y suffit pas, puisque la requête part bien de la machine ;
 * seul l'en-tête {@code Host}, qui porte alors le nom de l'attaquant, la trahit.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class LocalHostOnlyFilter extends OncePerRequestFilter {

    private static final Set<String> LOCAL_HOST_NAMES = Set.of("localhost", "127.0.0.1");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!LOCAL_HOST_NAMES.contains(request.getServerName().toLowerCase(Locale.ROOT))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }
}
