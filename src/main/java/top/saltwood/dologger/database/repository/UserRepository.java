package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserRepository {

    private static final String INSERT_OR_UPDATE_NAME = """
            INSERT INTO users(name, uuid) VALUES(?, ?)
            """ + SchemaCreator.onConflictDoUpdate("uuid", "name = ?");

    private static final String INSERT_NON_PLAYER = """
            INSERT INTO users(name) VALUES(?)
            """ + SchemaCreator.onConflictDoNothing();

    private static final String SELECT_ALL_USERNAMES = "SELECT id, name FROM users";

    public void insertOrUpdateName(String name, UUID uuid) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_OR_UPDATE_NAME)) {
                stmt.setString(1, name);
                stmt.setObject(2, uuid);
                stmt.setString(3, name);
                stmt.executeUpdate();
            }
        });
    }

    public void insertNonPlayer(String name) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT_NON_PLAYER)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
        });
    }

    public Map<Integer, String> getAllUsernames() throws SQLException {
        Map<Integer, String> usernames = new HashMap<>();
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_USERNAMES); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usernames.put(rs.getInt("id"), rs.getString("name"));
            }
        }
        return usernames;
    }
}
