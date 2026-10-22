package io.orewise.api.tenant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void platformAdminManagesTenantsAndMembersSeeTheirTenant() throws Exception {
        mvc.perform(post("/api/admin/tenants").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"acme","name":"Acme NV","plan":"BASIC"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.docLimit").value(1000))
                .andExpect(jsonPath("$.storageLimitBytes").value(2147483648L));

        mvc.perform(post("/api/admin/tenants").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"acme","name":"Again","plan":"PRO"}"""))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/admin/tenants/acme/plan").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"plan":"PRO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.docLimit").value(10000));

        mvc.perform(get("/api/me").with(jwt()
                        .jwt(j -> j.claim("preferred_username", "jan").claim("groups", List.of("/tenants/acme/hr")))
                        .authorities(new SimpleGrantedAuthority("ROLE_tenant-admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("jan"))
                .andExpect(jsonPath("$.roles[0]").value("tenant-admin"))
                .andExpect(jsonPath("$.tenant.slug").value("acme"))
                .andExpect(jsonPath("$.tenant.plan").value("PRO"));
    }

    @Test
    void tenantAdminCannotManageTenants() throws Exception {
        mvc.perform(get("/api/admin/tenants").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_tenant-admin"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsRejectedButHealthIsOpen() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void invalidSlugIsRejected() throws Exception {
        mvc.perform(post("/api/admin/tenants").with(platformAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slug":"Bad Slug!","name":"x","plan":"BASIC"}"""))
                .andExpect(status().isBadRequest());
    }

    private static RequestPostProcessor platformAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_platform-admin"));
    }
}
