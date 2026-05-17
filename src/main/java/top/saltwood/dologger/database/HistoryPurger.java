package top.saltwood.dologger.database;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;

public class HistoryPurger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String[] HISTORY_TABLES = {
            "blocks",
            "containers",
            "items",
            "sessions",
            "chats",
            "commands"
    };

    private HistoryPurger() {
    }

    public static int purgeOlderThan(Connection connection, int retentionDays) throws SQLException {
        if (retentionDays <= 0) {
            return 0;
        }

        long cutoffMillis = System.currentTimeMillis() - Duration.ofDays(retentionDays).toMillis();
        boolean originalAutoCommit = connection.getAutoCommit();
        int deletedRows = 0;

        try {
            connection.setAutoCommit(false);
            for (String table : HISTORY_TABLES) {
                deletedRows += deleteOlderThan(connection, table, cutoffMillis);
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(originalAutoCommit);
        }

        LOGGER.info("DoLogger: Purged history older than {} days (cutoff {}, deleted {} rows)", retentionDays, cutoffMillis, deletedRows);
        return deletedRows;
    }

    private static int deleteOlderThan(Connection connection, String table, long cutoffMillis) throws SQLException {
        if ("blocks".equals(table)) {
            return deleteOldBlocks(connection, cutoffMillis);
        }
        try (PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table + " WHERE time < ?")) {
            statement.setLong(1, cutoffMillis);
            return statement.executeUpdate();
        }
    }

    private static int deleteOldBlocks(Connection connection, long cutoffMillis) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                DELETE FROM blocks
                WHERE time < ?
                AND NOT (reverted_at IS NOT NULL AND restored_at IS NULL)
                """)) {
            statement.setLong(1, cutoffMillis);
            return statement.executeUpdate();
        }
    }
}
