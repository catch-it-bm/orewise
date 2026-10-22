package io.orewise.core.tenant;

/** Prices (EUR excl. VAT) live in billing/marketing; this holds the enforced limits. */
public enum Plan {
    BASIC(2_500, gb(5), 2_000),
    PRO(10_000, gb(20), 7_500),
    BUSINESS(50_000, gb(100), 20_000),
    /** Self-host / contract: limits are set per tenant (license); defaults equal BUSINESS. */
    ENTERPRISE(50_000, gb(100), 20_000);

    private final int defaultDocLimit;
    private final long defaultStorageLimitBytes;
    private final int defaultMonthlyQuestionLimit;

    Plan(int defaultDocLimit, long defaultStorageLimitBytes, int defaultMonthlyQuestionLimit) {
        this.defaultDocLimit = defaultDocLimit;
        this.defaultStorageLimitBytes = defaultStorageLimitBytes;
        this.defaultMonthlyQuestionLimit = defaultMonthlyQuestionLimit;
    }

    public int defaultDocLimit() {
        return defaultDocLimit;
    }

    public long defaultStorageLimitBytes() {
        return defaultStorageLimitBytes;
    }

    /** Fair-use allowance: drives usage warnings, not a hard cut-off. */
    public int defaultMonthlyQuestionLimit() {
        return defaultMonthlyQuestionLimit;
    }

    private static long gb(long n) {
        return n * 1024 * 1024 * 1024;
    }
}
