package io.orewise.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reads Orewise identity from a Keycloak access token. */
public final class KeycloakJwt {

    private static final String TENANT_GROUP_ROOT = "tenants";

    private KeycloakJwt() {
    }

    /** Realm roles ({@code realm_access.roles}) as {@code ROLE_<role>}. */
    public static Collection<GrantedAuthority> authorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    /**
     * Tenant slug from group membership {@code /tenants/<slug>[/<collection>...]}.
     * Empty when the user is in no tenant or in more than one.
     */
    public static Optional<String> tenantSlug(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("groups");
        if (groups == null) {
            return Optional.empty();
        }
        List<String> slugs = groups.stream()
                .map(g -> g.split("/"))
                .filter(p -> p.length >= 3 && p[0].isEmpty() && TENANT_GROUP_ROOT.equals(p[1]))
                .map(p -> p[2])
                .distinct()
                .toList();
        return slugs.size() == 1 ? Optional.of(slugs.getFirst()) : Optional.empty();
    }
}
