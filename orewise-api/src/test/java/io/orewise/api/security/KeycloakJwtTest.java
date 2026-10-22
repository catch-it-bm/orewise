package io.orewise.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtTest {

    @Test
    void mapsRealmRolesToAuthorities() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("tenant-admin", "offline_access"))));

        assertThat(KeycloakJwt.authorities(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_tenant-admin", "ROLE_offline_access");
    }

    @Test
    void noRealmAccessMeansNoAuthorities() {
        assertThat(KeycloakJwt.authorities(jwt(Map.of()))).isEmpty();
    }

    @Test
    void tenantSlugFromGroupPaths() {
        assertThat(slug(List.of("/tenants/acme", "/tenants/acme/hr", "/other"))).contains("acme");
        assertThat(slug(List.of("/other", "/tenantsx/acme"))).isEmpty();
        assertThat(slug(List.of("/tenants/acme", "/tenants/globex"))).isEmpty();
    }

    private static Optional<String> slug(List<String> groups) {
        return KeycloakJwt.tenantSlug(jwt(Map.of("groups", groups)));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u");
        claims.forEach(b::claim);
        return b.build();
    }
}
