package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;

import java.sql.PreparedStatement;

public class EntityRepository {

    private static final String INSERT = """
            INSERT INTO entities(name) VALUES(?)
            """ + SchemaCreator.onConflictDoNothing();

    public void insert(String name) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setString(1, name);
                stmt.executeUpdate();
            }
        });
    }
}
