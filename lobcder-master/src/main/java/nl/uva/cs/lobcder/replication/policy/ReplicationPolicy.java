package nl.uva.cs.lobcder.replication.policy;

import java.sql.Connection;
import java.util.Collection;

/**
 * Created by dvasunin on 20.01.15.
 */
public interface ReplicationPolicy {
    public Collection<Long> getSitesToReplicate(Connection connection) throws Exception;
}
