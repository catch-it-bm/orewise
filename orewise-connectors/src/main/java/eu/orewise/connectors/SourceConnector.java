package eu.orewise.connectors;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * A document source (upload, Dropbox, mounted folder, ...). Implementations are stateless;
 * sync state lives in the {@link SyncCursor} persisted by the worker.
 */
public interface SourceConnector {

    String type();

    /** Returns one page of changes; call again with {@link ChangeSet#next()} while {@code hasMore}. */
    ChangeSet listChanges(ConnectorConfig config, SyncCursor since);

    InputStream fetch(ConnectorConfig config, SourceItem item);

    record ConnectorConfig(String connectorId, String tenantId, Map<String, String> settings) {
    }

    /** {@code value} is opaque to the worker; {@code null} means full sync. */
    record SyncCursor(String value) {
    }

    record SourceItem(String externalId, String path, String contentHash, long sizeBytes,
                      Instant modifiedAt, boolean deleted) {
    }

    record ChangeSet(List<SourceItem> items, SyncCursor next, boolean hasMore) {
    }
}
