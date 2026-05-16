package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;

import java.sql.PreparedStatement;

public class MaterialRepository {

    private static final String INSERT = """
            INSERT INTO materials(name) VALUES(?)
            """ + SchemaCreator.onConflictDoNothing();

    public void insert(String name) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
                stmt.setString(1, stripMinecraftNamespace(name));
                stmt.executeUpdate();
            }
        });
    }

    static String stripMinecraftNamespace(String name) {
        return name != null && name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }
}
