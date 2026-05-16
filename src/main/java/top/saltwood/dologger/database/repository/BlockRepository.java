package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;
import top.saltwood.dologger.model.history.BlockHistory;
import top.saltwood.dologger.model.action.BlockAction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BlockRepository {

    private static final String UPSERT_MATERIAL = "INSERT INTO materials(name) VALUES(?) " + SchemaCreator.onConflictDoNothing();
    private static final String UPSERT_ENTITY = "INSERT INTO entities(name) VALUES(?) " + SchemaCreator.onConflictDoNothing();
    private static final String INSERT_MATERIAL_BLOCK = """
            INSERT INTO blocks(time, user_id, level, x, y, z, type, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM materials WHERE name = ?), ?)
            """;
    private static final String INSERT_ENTITY_BLOCK = """
            INSERT INTO blocks(time, user_id, level, x, y, z, type, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM entities WHERE name = ?), ?)
            """;
    private static final String SELECT_BLOCK_HISTORY = """
            SELECT b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ? AND b.x = ? AND b.y = ? AND b.z = ? AND b.action IN (0, 1)
            ORDER BY b.time DESC
            """;
    private static final String SELECT_INTERACTION_HISTORY = """
            SELECT b.time, u.name, u.uuid, b.x, b.y, b.z, COALESCE(m.name, e.name) AS material, b.action
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            LEFT JOIN materials m ON b.type = m.id AND b.action = 2
            LEFT JOIN entities e ON b.type = e.id AND b.action = 4
            WHERE l.name = ? AND b.x = ? AND b.y = ? AND b.z = ? AND b.action IN (2, 4)
            ORDER BY b.time DESC
            """;
    private static final String DELETE_INTERACTIONS = """
            DELETE FROM blocks
            WHERE action = 2 AND level = (SELECT id FROM levels WHERE name = ?) AND x = ? AND y = ? AND z = ?
            """;
    private static final String SELECT_FILTERED = """
            SELECT b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ?
            AND (? IS NULL OR u.name = ?)
            AND (? IS NULL OR b.time >= ?)
            AND (? IS NULL OR b.time <= ?)
            AND (? IS NULL OR b.x = ?)
            AND (? IS NULL OR b.y = ?)
            AND (? IS NULL OR b.z = ?)
            AND (? IS NULL OR b.action = ?)
            AND (? IS NULL OR m.name = ?)
            ORDER BY b.time DESC LIMIT 1000
            """;

    public void insertMaterial(long time, UUID userUuid, String levelName, int x, int y, int z, String material, BlockAction action) {
        String materialName = MaterialRepository.stripMinecraftNamespace(material);
        Dologger.getSqlQueue().enqueue(conn -> {
            upsertNamed(conn, UPSERT_MATERIAL, materialName);
            insert(conn, INSERT_MATERIAL_BLOCK, time, userUuid, levelName, x, y, z, materialName, action);
        });
    }

    public void insertEntity(long time, UUID userUuid, String levelName, int x, int y, int z, String entity, BlockAction action) {
        Dologger.getSqlQueue().enqueue(conn -> {
            upsertNamed(conn, UPSERT_ENTITY, entity);
            insert(conn, INSERT_ENTITY_BLOCK, time, userUuid, levelName, x, y, z, entity, action);
        });
    }

    public List<BlockHistory> getBlockHistory(String levelName, int x, int y, int z) throws SQLException {
        return queryPositionHistory(SELECT_BLOCK_HISTORY, levelName, x, y, z);
    }

    public List<BlockHistory> getInteractionHistory(String levelName, int x, int y, int z) throws SQLException {
        return queryPositionHistory(SELECT_INTERACTION_HISTORY, levelName, x, y, z);
    }

    public void removeInteractionsForPosition(String levelName, int x, int y, int z) {
        Dologger.getSqlQueue().enqueue(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(DELETE_INTERACTIONS)) {
                stmt.setString(1, levelName);
                stmt.setInt(2, x);
                stmt.setInt(3, y);
                stmt.setInt(4, z);
                stmt.executeUpdate();
            }
        });
    }

    public List<BlockHistory> getFilteredBlockHistory(String levelName, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_FILTERED)) {
            stmt.setString(1, levelName);
            bindNullGuardFilters(stmt, 2, filters, 8);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapBlockHistory(rs);
            }
        }
    }

    private static void upsertNamed(Connection conn, String sql, String name) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
        }
    }

    private static void insert(Connection conn, String sql, long time, UUID userUuid, String levelName, int x, int y, int z, String type, BlockAction action) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, time);
            stmt.setObject(2, userUuid);
            stmt.setString(3, levelName);
            stmt.setInt(4, x);
            stmt.setInt(5, y);
            stmt.setInt(6, z);
            stmt.setString(7, type);
            stmt.setInt(8, action.getId());
            stmt.executeUpdate();
        }
    }

    static void bindNullGuardFilters(PreparedStatement stmt, int start, List<Object> filters, int count) throws SQLException {
        for (int i = 0; i < count; i++) {
            Object value = filters != null && i < filters.size() ? filters.get(i) : null;
            stmt.setObject(start + (i * 2), value);
            stmt.setObject(start + (i * 2) + 1, value);
        }
    }

    private static List<BlockHistory> queryPositionHistory(String sql, String levelName, int x, int y, int z) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, levelName);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapBlockHistory(rs);
            }
        }
    }

    private static List<BlockHistory> mapBlockHistory(ResultSet rs) throws SQLException {
        List<BlockHistory> history = new ArrayList<>();
        while (rs.next()) {
            history.add(new BlockHistory(RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), rs.getString("material"), BlockAction.fromId(rs.getInt("action"))));
        }
        return history;
    }
}
