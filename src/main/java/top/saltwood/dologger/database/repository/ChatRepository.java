package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.model.history.ChatHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatRepository {

    private static final String INSERT = """
            INSERT INTO chats(time, user_id, level, x, y, z, message)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, ?)
            """;
    private static final String SELECT_FILTERED = """
            SELECT c.time, u.name, u.uuid, c.x, c.y, c.z, c.message
            FROM chats c
            JOIN users u ON c.user_id = u.id
            JOIN levels l ON c.level = l.id
            WHERE l.name = ?
            AND (? OR u.name = ANY(?::text[]) OR u.uuid IN (SELECT un.uuid FROM usernames un WHERE un.name = ANY(?::text[])))
            AND (? OR c.time >= ?)
            AND (? OR c.time <= ?)
            AND (? OR c.x BETWEEN ? AND ?)
            AND (? OR c.y BETWEEN ? AND ?)
            AND (? OR c.z BETWEEN ? AND ?)
            ORDER BY c.time DESC LIMIT 1000
            """;

    public void insert(long time, UUID userUuid, String levelName, int x, int y, int z, String message) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setLong(1, time);
                stmt.setObject(2, userUuid);
                stmt.setString(3, levelName);
                stmt.setInt(4, x);
                stmt.setInt(5, y);
                stmt.setInt(6, z);
                stmt.setString(7, message);
                stmt.executeUpdate();
            }
        });
    }

    public List<ChatHistory> getFilteredChatHistory(String levelName, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_FILTERED)) {
            stmt.setString(1, levelName);
            BlockRepository.bindUserTimeRadiusFilters(stmt, 2, filters);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ChatHistory> history = new ArrayList<>();
                while (rs.next()) {
                    history.add(new ChatHistory(RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), rs.getString("message")));
                }
                return history;
            }
        }
    }
}
