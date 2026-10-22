package io.orewise.api.tenant;

import io.orewise.core.tenant.Plan;
import io.orewise.core.tenant.Tenant;

public record TenantView(String slug, String name, Plan plan, int docLimit, long storageLimitBytes) {

    public static TenantView from(Tenant t) {
        return new TenantView(t.getSlug(), t.getName(), t.getPlan(), t.getDocLimit(), t.getStorageLimitBytes());
    }
}
