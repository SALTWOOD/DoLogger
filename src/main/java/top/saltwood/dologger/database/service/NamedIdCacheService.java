package top.saltwood.dologger.database.service;

import top.saltwood.dologger.Dologger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class NamedIdCacheService {

    private final Map<String, Integer> ids = new ConcurrentHashMap<>();
    private final String tableName;

    NamedIdCacheService(String tableName) {
        this.tableName = tableName;
    }

    public Integer getId(String name) {
        String normalized = normalize(name);
        if (normalized == null || !ServiceSupport.canRead()) {
            return null;
        }
        Integer cached = ids.get(normalized);
        if (cached != null) {
            return cached;
        }
        try (var connection = Dologger.getDatabaseManager().getConnection(); PreparedStatement stmt = connection.prepareStatement("SELECT id FROM " + tableName + " WHERE name = ?")) {
            stmt.setString(1, normalized);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    ids.put(normalized, id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
        }
        return null;
    }

    public void clearCache() {
        ids.clear();
    }

    protected String normalize(String name) {
        return name == null || name.isBlank() ? null : name;
    }

    protected void rememberPending(String name) {
        String normalized = normalize(name);
        if (normalized != null) {
            ids.remove(normalized);
        }
    }
}
