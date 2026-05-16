package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;

import java.sql.PreparedStatement;
import java.util.UUID;

public class ChatRepository {

    private static final String INSERT = """
            INSERT INTO chats(time, user_id, level, x, y, z, message)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, ?)
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
}
