package top.saltwood.dologger.database;

import com.mojang.logging.LogUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import top.saltwood.dologger.Config;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long ERROR_LOG_INTERVAL_MS = 30_000;

    private HikariDataSource dataSource;
    private volatile boolean available;
    private long lastErrorLogTime;

    public void start() {
        if (!Config.enabled) {
            LOGGER.info("DoLogger: Database logging disabled by config");
            return;
        }

        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:postgresql://" + Config.host + ":" + Config.port + "/" + Config.database);
            config.setUsername(Config.username);
            config.setPassword(Config.password);
            config.setMaximumPoolSize(Config.poolSize);
            config.setConnectionTimeout(Config.connectionTimeout);
            config.setValidationTimeout(Config.validationTimeout);
            config.setIdleTimeout(Config.idleTimeout);
            config.setMaxLifetime(Config.maxLifetime);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("DoLogger-HikariCP");

            dataSource = new HikariDataSource(config);

            if (isConnectionHealthy()) {
                try (Connection connection = dataSource.getConnection()) {
                    SchemaCreator.createSchema(connection);
                    HistoryPurger.purgeOlderThan(connection, Config.purgeRetentionDays);
                    available = true;
                    LOGGER.info("DoLogger: Connected to PostgreSQL at {}:{}/{}", Config.host, Config.port, Config.database);
                }
            } else {
                available = false;
                LOGGER.error("DoLogger: Failed to validate PostgreSQL connection at {}:{}/{}", Config.host, Config.port, Config.database);
                closeDataSource();
            }
        } catch (Exception e) {
            available = false;
            LOGGER.error("DoLogger: PostgreSQL unavailable at {}:{}/{}; database logging skipped - {}", Config.host, Config.port, Config.database, e.getMessage());
            closeDataSource();
        }
    }

    public void stop() {
        available = false;
        closeDataSource();
        LOGGER.info("DoLogger: Database connection pool closed");
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean isConnectionHealthy() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            return false;
        }

        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(Math.max(1, Config.validationTimeout / 1000));
        }
    }

    public Connection getConnection() throws SQLException {
        if (!available || dataSource == null) {
            throw new SQLException("Database is not available");
        }

        return dataSource.getConnection();
    }

    public boolean tryReconnect() {
        if (available) {
            return true;
        }

        try {
            closeDataSource();
            start();
            return available;
        } catch (Exception e) {
            logBoundedError("Reconnect failed: " + e.getMessage());
            return false;
        }
    }

    public void logBoundedError(String message) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogTime >= ERROR_LOG_INTERVAL_MS) {
            lastErrorLogTime = now;
            LOGGER.error("DoLogger: {}", message);
        }
    }

    public boolean executeIfAvailable(DatabaseTask task) {
        if (!available) {
            logBoundedError("Database unavailable, skipping operation");
            return false;
        }

        try (Connection connection = getConnection()) {
            task.execute(connection);
            return true;
        } catch (SQLException e) {
            available = false;
            logBoundedError("Database operation failed: " + e.getMessage());
            return false;
        }
    }

    private void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }

    @FunctionalInterface
    public interface DatabaseTask {
        void execute(Connection connection) throws SQLException;
    }
}
