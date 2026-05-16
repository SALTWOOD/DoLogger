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
    private static final String INSERT_GENERATED_MATERIAL_BLOCK = """
            INSERT INTO blocks(time, user_id, level, x, y, z, type, action, command_generated, source_block_id)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM materials WHERE name = ?), ?, true, ?)
            """;
    private static final String INSERT_ENTITY_BLOCK = """
            INSERT INTO blocks(time, user_id, level, x, y, z, type, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM entities WHERE name = ?), ?)
            """;
    private static final String SELECT_BLOCK_HISTORY = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ? AND b.x = ? AND b.y = ? AND b.z = ? AND b.action IN (0, 1)
            ORDER BY b.time DESC
            """;
    private static final String SELECT_INTERACTION_HISTORY = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ? AND b.x = ? AND b.y = ? AND b.z = ? AND b.action = 2
            ORDER BY b.time DESC
            """;
    private static final String SELECT_ENTITY_HISTORY = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, e.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN entities e ON b.type = e.id
            WHERE l.name = ? AND b.x = ? AND b.y = ? AND b.z = ? AND b.action IN (3, 4)
            ORDER BY b.time DESC
            """;
    private static final String DELETE_INTERACTIONS = """
            DELETE FROM blocks
            WHERE action = 2 AND level = (SELECT id FROM levels WHERE name = ?) AND x = ? AND y = ? AND z = ?
            """;
    private static final String SELECT_FILTERED = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ?
            AND (? OR u.name = ANY(?::text[]) OR u.uuid IN (SELECT un.uuid FROM usernames un WHERE un.name = ANY(?::text[])))
            AND (? OR b.time >= ?)
            AND (? OR b.time <= ?)
            AND (? OR b.x BETWEEN ? AND ?)
            AND (? OR b.y BETWEEN ? AND ?)
            AND (? OR b.z BETWEEN ? AND ?)
            AND (? OR b.action = ANY(?::integer[]))
            AND (? OR m.name = ANY(?::text[]))
            AND (? OR NOT (m.name = ANY(?::text[])))
            ORDER BY b.time DESC LIMIT 1000
            """;
    private static final String SELECT_REVERT_CANDIDATES = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ?
            AND b.action IN (0, 1)
            AND b.command_generated = false
            AND (b.reverted_at IS NULL OR b.restored_at IS NOT NULL)
            AND (? OR u.name = ANY(?::text[]) OR u.uuid IN (SELECT un.uuid FROM usernames un WHERE un.name = ANY(?::text[])))
            AND (? OR b.time >= ?)
            AND (? OR b.time <= ?)
            AND (? OR b.x BETWEEN ? AND ?)
            AND (? OR b.y BETWEEN ? AND ?)
            AND (? OR b.z BETWEEN ? AND ?)
            AND (? OR b.action = ANY(?::integer[]))
            AND (? OR m.name = ANY(?::text[]))
            AND (? OR NOT (m.name = ANY(?::text[])))
            ORDER BY b.time DESC, b.id DESC
            """;
    private static final String SELECT_RESTORE_CANDIDATES = """
            SELECT b.id, b.time, u.name, u.uuid, b.x, b.y, b.z, m.name AS material, b.action, b.reverted_at, b.restored_at
            FROM blocks b
            JOIN users u ON b.user_id = u.id
            JOIN levels l ON b.level = l.id
            JOIN materials m ON b.type = m.id
            WHERE l.name = ?
            AND b.action IN (0, 1)
            AND b.command_generated = false
            AND b.reverted_at IS NOT NULL
            AND b.restored_at IS NULL
            AND (? OR u.name = ANY(?::text[]) OR u.uuid IN (SELECT un.uuid FROM usernames un WHERE un.name = ANY(?::text[])))
            AND (? OR b.time >= ?)
            AND (? OR b.time <= ?)
            AND (? OR b.x BETWEEN ? AND ?)
            AND (? OR b.y BETWEEN ? AND ?)
            AND (? OR b.z BETWEEN ? AND ?)
            AND (? OR b.action = ANY(?::integer[]))
            AND (? OR m.name = ANY(?::text[]))
            AND (? OR NOT (m.name = ANY(?::text[])))
            ORDER BY b.time ASC, b.id ASC
            """;
    private static final String MARK_REVERTED = """
            UPDATE blocks
            SET reverted_at = ?, reverted_by = (SELECT id FROM users WHERE uuid = ?), revert_batch = ?, restored_at = NULL
            WHERE id = ? AND command_generated = false AND (reverted_at IS NULL OR restored_at IS NOT NULL)
            """;
    private static final String MARK_RESTORED = """
            UPDATE blocks
            SET restored_at = ?, restored_by = (SELECT id FROM users WHERE uuid = ?), restore_batch = ?, reverted_at = NULL
            WHERE id = ? AND command_generated = false AND reverted_at IS NOT NULL AND restored_at IS NULL
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
        List<BlockHistory> history = new ArrayList<>(queryPositionHistory(SELECT_INTERACTION_HISTORY, levelName, x, y, z));
        history.addAll(queryPositionHistory(SELECT_ENTITY_HISTORY, levelName, x, y, z));
        return history;
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
            bindHistoryFilters(stmt, 2, filters, true);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapBlockHistory(rs);
            }
        }
    }

    public List<BlockHistory> getRevertCandidates(String levelName, List<Object> filters) throws SQLException {
        return queryCandidates(SELECT_REVERT_CANDIDATES, levelName, filters);
    }

    public List<BlockHistory> getRestoreCandidates(String levelName, List<Object> filters) throws SQLException {
        return queryCandidates(SELECT_RESTORE_CANDIDATES, levelName, filters);
    }

    public boolean markReverted(int id, UUID actorUuid, long at, UUID batch) throws SQLException {
        return mark(MARK_REVERTED, id, actorUuid, at, batch);
    }

    public boolean markRestored(int id, UUID actorUuid, long at, UUID batch) throws SQLException {
        return mark(MARK_RESTORED, id, actorUuid, at, batch);
    }

    public void insertGeneratedMaterial(long time, UUID userUuid, String levelName, int x, int y, int z, String material, BlockAction action, int sourceBlockId) {
        String materialName = MaterialRepository.stripMinecraftNamespace(material);
        Dologger.getSqlQueue().enqueue(conn -> {
            upsertNamed(conn, UPSERT_MATERIAL, materialName);
            insertGenerated(conn, time, userUuid, levelName, x, y, z, materialName, action, sourceBlockId);
        });
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

    private static void insertGenerated(Connection conn, long time, UUID userUuid, String levelName, int x, int y, int z, String material, BlockAction action, int sourceBlockId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_GENERATED_MATERIAL_BLOCK)) {
            stmt.setLong(1, time);
            stmt.setObject(2, userUuid);
            stmt.setString(3, levelName);
            stmt.setInt(4, x);
            stmt.setInt(5, y);
            stmt.setInt(6, z);
            stmt.setString(7, material);
            stmt.setInt(8, action.getId());
            stmt.setInt(9, sourceBlockId);
            stmt.executeUpdate();
        }
    }

    static void bindHistoryFilters(PreparedStatement stmt, int start, List<Object> filters, boolean includeMaterialFilters) throws SQLException {
        int index = start;
        index = bindUserFilter(stmt, index, value(filters, 0));
        index = bindPair(stmt, index, value(filters, 1));
        index = bindPair(stmt, index, value(filters, 2));
        index = bindBetween(stmt, index, value(filters, 3), value(filters, 4));
        index = bindBetween(stmt, index, value(filters, 5), value(filters, 6));
        index = bindBetween(stmt, index, value(filters, 7), value(filters, 8));
        index = bindIntArray(stmt, index, value(filters, 9));
        if (includeMaterialFilters) {
            index = bindTextArray(stmt, index, value(filters, 10));
            bindTextArray(stmt, index, value(filters, 11));
        }
    }

    private static Object value(List<Object> filters, int index) {
        return filters != null && index < filters.size() ? filters.get(index) : null;
    }

    private static int bindPair(PreparedStatement stmt, int index, Object value) throws SQLException {
        boolean disabled = value == null;
        stmt.setBoolean(index++, disabled);
        if (value instanceof Long longValue) {
            stmt.setLong(index++, longValue);
        } else if (value instanceof Integer intValue) {
            stmt.setInt(index++, intValue);
        } else {
            stmt.setLong(index++, 0L);
        }
        return index;
    }

    private static int bindBetween(PreparedStatement stmt, int index, Object min, Object max) throws SQLException {
        boolean disabled = min == null || max == null;
        stmt.setBoolean(index++, disabled);
        stmt.setInt(index++, min instanceof Integer value ? value : 0);
        stmt.setInt(index++, max instanceof Integer value ? value : 0);
        return index;
    }

    private static int bindIntArray(PreparedStatement stmt, int index, Object value) throws SQLException {
        boolean disabled = !(value instanceof int[]);
        stmt.setBoolean(index++, disabled);
        if (value instanceof int[] values) {
            Integer[] boxed = java.util.Arrays.stream(values).boxed().toArray(Integer[]::new);
            java.sql.Array array = stmt.getConnection().createArrayOf("integer", boxed);
            stmt.setArray(index++, array);
        } else {
            stmt.setArray(index++, stmt.getConnection().createArrayOf("integer", new Integer[0]));
        }
        return index;
    }

    private static int bindUserFilter(PreparedStatement stmt, int index, Object value) throws SQLException {
        boolean disabled = !(value instanceof String[]);
        stmt.setBoolean(index++, disabled);
        if (value instanceof String[] values) {
            java.sql.Array array = stmt.getConnection().createArrayOf("text", values);
            stmt.setArray(index++, array);
            stmt.setArray(index++, array);
        } else {
            java.sql.Array array = stmt.getConnection().createArrayOf("text", new String[0]);
            stmt.setArray(index++, array);
            stmt.setArray(index++, array);
        }
        return index;
    }

    private static int bindTextArray(PreparedStatement stmt, int index, Object value) throws SQLException {
        boolean disabled = !(value instanceof String[]);
        stmt.setBoolean(index++, disabled);
        if (value instanceof String[] values) {
            java.sql.Array array = stmt.getConnection().createArrayOf("text", values);
            stmt.setArray(index++, array);
        } else {
            stmt.setArray(index++, stmt.getConnection().createArrayOf("text", new String[0]));
        }
        return index;
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

    private static List<BlockHistory> queryCandidates(String sql, String levelName, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, levelName);
            bindHistoryFilters(stmt, 2, filters, true);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapBlockHistory(rs);
            }
        }
    }

    private static boolean mark(String sql, int id, UUID actorUuid, long at, UUID batch) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, at);
            stmt.setObject(2, actorUuid);
            stmt.setObject(3, batch);
            stmt.setInt(4, id);
            return stmt.executeUpdate() == 1;
        }
    }

    private static List<BlockHistory> mapBlockHistory(ResultSet rs) throws SQLException {
        List<BlockHistory> history = new ArrayList<>();
        while (rs.next()) {
            long revertedAt = rs.getLong("reverted_at");
            Long nullableRevertedAt = rs.wasNull() ? null : revertedAt;
            long restoredAt = rs.getLong("restored_at");
            Long nullableRestoredAt = rs.wasNull() ? null : restoredAt;
            history.add(new BlockHistory(rs.getInt("id"), RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), rs.getString("material"), BlockAction.fromId(rs.getInt("action")), nullableRevertedAt, nullableRestoredAt));
        }
        return history;
    }
}
