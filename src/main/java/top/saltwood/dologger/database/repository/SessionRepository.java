package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.model.action.SessionAction;
import top.saltwood.dologger.model.history.SessionHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionRepository {

    private static final String INSERT = """
            INSERT INTO sessions(time, user_id, level, x, y, z, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, ?)
            """;
    private static final String SELECT_FILTERED = """
            SELECT s.time, u.name, u.uuid, s.x, s.y, s.z, s.action
            FROM sessions s
            JOIN users u ON s.user_id = u.id
            JOIN levels l ON s.level = l.id
            WHERE l.name = ?
            AND (? IS NULL OR u.name = ?)
            AND (? IS NULL OR s.time >= ?)
            AND (? IS NULL OR s.time <= ?)
            AND (? IS NULL OR s.x BETWEEN ? AND ?)
            AND (? IS NULL OR s.y BETWEEN ? AND ?)
            AND (? IS NULL OR s.z BETWEEN ? AND ?)
            AND (? IS NULL OR s.action = ANY(?))
            ORDER BY s.time DESC LIMIT 1000
            """;

    public void insert(long time, UUID userUuid, String levelName, int x, int y, int z, SessionAction action) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setLong(1, time);
                stmt.setObject(2, userUuid);
                stmt.setString(3, levelName);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setInt(7, action.getId());
                stmt.executeUpdate();
            }
        });
    }

    public List<SessionHistory> getFilteredSessionHistory(String levelName, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_FILTERED)) {
            stmt.setString(1, levelName);
            BlockRepository.bindHistoryFilters(stmt, 2, filters, false);
            try (ResultSet rs = stmt.executeQuery()) {
                List<SessionHistory> history = new ArrayList<>();
                while (rs.next()) {
                    history.add(new SessionHistory(RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), SessionAction.fromId(rs.getInt("action"))));
                }
                return history;
            }
        }
    }
}
