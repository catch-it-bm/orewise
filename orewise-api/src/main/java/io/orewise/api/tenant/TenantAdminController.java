package io.orewise.api.tenant;

import io.orewise.core.tenant.Plan;
import io.orewise.core.tenant.Tenant;
import io.orewise.core.tenant.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Platform-admin only (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin/tenants")
public class TenantAdminController {

    private final TenantRepository tenants;

    public TenantAdminController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    public record CreateTenant(
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9-]{1,62}") String slug,
            @NotBlank String name,
            @NotNull Plan plan) {
    }

    public record ChangePlan(@NotNull Plan plan) {
    }

    @GetMapping
    public List<TenantView> list() {
        return tenants.findAll().stream().map(TenantView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantView create(@Valid @RequestBody CreateTenant req) {
        if (tenants.existsBySlug(req.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant '" + req.slug() + "' already exists");
        }
        return TenantView.from(tenants.save(new Tenant(req.slug(), req.name(), req.plan())));
    }

    @PutMapping("/{slug}/plan")
    @Transactional
    public TenantView changePlan(@PathVariable String slug, @Valid @RequestBody ChangePlan req) {
        Tenant tenant = tenants.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tenant.changePlan(req.plan());
        return TenantView.from(tenant);
    }
}
