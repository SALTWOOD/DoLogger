package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;

import java.sql.PreparedStatement;
import java.util.UUID;

public class UsernameRepository {

    private static final String INSERT = """
            INSERT INTO usernames(time, uuid, name) VALUES(?, ?, ?)
            """ + SchemaCreator.onConflictDoNothing();

    public boolean insert(long time, UUID uuid, String name) {
        return Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setLong(1, time);
                stmt.setObject(2, uuid);
                stmt.setString(3, name);
                stmt.executeUpdate();
            }
        });
    }
}
