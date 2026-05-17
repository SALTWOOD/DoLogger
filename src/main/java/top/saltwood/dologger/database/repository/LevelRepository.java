package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;

import java.sql.PreparedStatement;

public class LevelRepository {

    private static final String INSERT = """
            INSERT INTO levels(name) VALUES(?)
            """ + SchemaCreator.onConflictDoNothing();

    public boolean insert(String name) {
        return Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
        });
    }
}
