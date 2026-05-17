package top.saltwood.dologger.database.repository;

import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SqlQueue;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.ItemHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemRepository {

    private static final String INSERT = """
            INSERT INTO items(time, user_id, level, x, y, z, type, data, amount, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM materials WHERE name = ?), ?, ?, ?)
            """;
    private static final String SELECT_FILTERED = """
            SELECT i.time, u.name, u.uuid, i.x, i.y, i.z, m.name AS material, i.data, i.amount, i.action
            FROM items i
            JOIN users u ON i.user_id = u.id
            JOIN levels l ON i.level = l.id
            JOIN materials m ON i.type = m.id
            WHERE l.name = ?
            AND (? OR u.name = ANY(?::text[]) OR u.uuid IN (SELECT un.uuid FROM usernames un WHERE un.name = ANY(?::text[])))
            AND (? OR i.time >= ?)
            AND (? OR i.time <= ?)
            AND (? OR i.x BETWEEN ? AND ?)
            AND (? OR i.y BETWEEN ? AND ?)
            AND (? OR i.z BETWEEN ? AND ?)
            AND (? OR i.action = ANY(?::integer[]))
            AND (? OR m.name = ANY(?::text[]))
            AND (? OR NOT (m.name = ANY(?::text[])))
            ORDER BY i.time DESC LIMIT 1000
            """;

    public boolean insert(long time, UUID userUuid, String level, int x, int y, int z, SimpleItemStack item, ItemAction action) {
        return Dologger.getSqlQueue().enqueue(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, action));
    }

    public boolean insertList(long time, UUID userUuid, String level, int x, int y, int z, List<SimpleItemStack> items, ItemAction action) {
        List<SqlQueue.SqlTask> tasks = new ArrayList<>();
        for (SimpleItemStack item : items) {
            tasks.add(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, action));
        }
        return Dologger.getSqlQueue().enqueueSqlBatch(tasks) == tasks.size();
    }

    public boolean insertMap(long time, UUID userUuid, String level, int x, int y, int z, Map<ItemAction, List<SimpleItemStack>> map) {
        List<SqlQueue.SqlTask> tasks = new ArrayList<>();
        for (Map.Entry<ItemAction, List<SimpleItemStack>> entry : map.entrySet()) {
            for (SimpleItemStack item : entry.getValue()) {
                tasks.add(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, entry.getKey()));
            }
        }
        return Dologger.getSqlQueue().enqueueSqlBatch(tasks) == tasks.size();
    }

    public List<ItemHistory> getFilteredItemHistory(String level, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_FILTERED)) {
            stmt.setString(1, level);
            BlockRepository.bindHistoryFilters(stmt, 2, filters, true);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapHistory(rs);
            }
        }
    }

    private static void insertOne(Connection conn, long time, UUID userUuid, String level, int x, int y, int z, SimpleItemStack item, ItemAction action) throws SQLException {
        String material = MaterialRepository.stripMinecraftNamespace(RepositoryMappers.itemName(item));
        try (PreparedStatement upsert = conn.prepareStatement(ContainerRepository.UPSERT_MATERIAL)) {
            upsert.setString(1, material);
            upsert.executeUpdate();
        }
        try (PreparedStatement stmt = conn.prepareStatement(INSERT)) {
            stmt.setLong(1, time);
            stmt.setObject(2, userUuid);
            stmt.setString(3, level);
            stmt.setInt(4, x);
            stmt.setInt(5, y);
            stmt.setInt(6, z);
            stmt.setString(7, material);
            stmt.setBytes(8, item.getTagBytes());
            stmt.setInt(9, item.getCount());
            stmt.setInt(10, action.getId());
            stmt.executeUpdate();
        }
    }

    private static List<ItemHistory> mapHistory(ResultSet rs) throws SQLException {
        List<ItemHistory> history = new ArrayList<>();
        while (rs.next()) {
            history.add(new ItemHistory(RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), RepositoryMappers.itemStack(rs), ItemAction.fromId(rs.getInt("action"))));
        }
        return history;
    }
}
