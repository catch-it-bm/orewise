package eu.orewise.api.tenant;

import eu.orewise.api.security.KeycloakJwt;
import eu.orewise.core.tenant.TenantRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    private final TenantRepository tenants;

    public MeController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public record Me(String username, List<String> roles, TenantView tenant) {
    }

    @GetMapping("/api/me")
    public Me me(JwtAuthenticationToken auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        TenantView tenant = KeycloakJwt.tenantSlug(auth.getToken())
                .flatMap(tenants::findBySlug)
                .map(TenantView::from)
                .orElse(null);
        return new Me(auth.getToken().getClaimAsString("preferred_username"), roles, tenant);
    }
}
