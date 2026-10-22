package eu.orewise.core.tenant;

public enum Plan {
    BASIC(1_000, gb(2)),
    PRO(10_000, gb(20)),
    BUSINESS(50_000, gb(100)),
    /** Limits are set per tenant (license / contract); defaults equal BUSINESS. */
    ENTERPRISE(50_000, gb(100));

    private final int defaultDocLimit;
    private final long defaultStorageLimitBytes;

    Plan(int defaultDocLimit, long defaultStorageLimitBytes) {
        this.defaultDocLimit = defaultDocLimit;
        this.defaultStorageLimitBytes = defaultStorageLimitBytes;
    }

    public int defaultDocLimit() {
        return defaultDocLimit;
    }

    public long defaultStorageLimitBytes() {
        return defaultStorageLimitBytes;
    }

    private static long gb(long n) {
        return n * 1024 * 1024 * 1024;
    }
}
