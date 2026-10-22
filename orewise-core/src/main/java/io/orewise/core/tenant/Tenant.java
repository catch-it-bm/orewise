package io.orewise.core.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    private UUID id;

    /** Matches the Keycloak group path {@code /tenants/<slug>}. */
    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(name = "doc_limit", nullable = false)
    private int docLimit;

    @Column(name = "storage_limit_bytes", nullable = false)
    private long storageLimitBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Tenant() {
    }

    public Tenant(String slug, String name, Plan plan) {
        this.id = UUID.randomUUID();
        this.slug = slug;
        this.name = name;
        this.createdAt = Instant.now();
        changePlan(plan);
    }

    /** Switches plan and resets limits to the plan defaults. */
    public void changePlan(Plan plan) {
        this.plan = plan;
        this.docLimit = plan.defaultDocLimit();
        this.storageLimitBytes = plan.defaultStorageLimitBytes();
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public Plan getPlan() {
        return plan;
    }

    public int getDocLimit() {
        return docLimit;
    }

    public long getStorageLimitBytes() {
        return storageLimitBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
