package top.saltwood.dologger.database.repository;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.SchemaCreator;
import top.saltwood.dologger.database.SqlQueue;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.Time;
import top.saltwood.dologger.model.User;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.ContainerHistory;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ContainerRepository {

    static final String UPSERT_MATERIAL = "INSERT INTO materials(name) VALUES(?) " + SchemaCreator.onConflictDoNothing();
    static final String INSERT = """
            INSERT INTO containers(time, user_id, level, x, y, z, type, data, amount, action)
            VALUES(?, (SELECT id FROM users WHERE uuid = ?), (SELECT id FROM levels WHERE name = ?), ?, ?, ?, (SELECT id FROM materials WHERE name = ?), ?, ?, ?)
            """;
    private static final String SELECT_POSITION = """
            SELECT c.time, u.name, u.uuid, c.x, c.y, c.z, m.name AS material, c.data, c.amount, c.action
            FROM containers c
            JOIN users u ON c.user_id = u.id
            JOIN levels l ON c.level = l.id
            JOIN materials m ON c.type = m.id
            WHERE l.name = ? AND c.x = ? AND c.y = ? AND c.z = ?
            ORDER BY c.time DESC
            """;
    private static final String SELECT_RANGE = """
            SELECT c.time, u.name, u.uuid, c.x, c.y, c.z, m.name AS material, c.data, c.amount, c.action
            FROM containers c
            JOIN users u ON c.user_id = u.id
            JOIN levels l ON c.level = l.id
            JOIN materials m ON c.type = m.id
            WHERE l.name = ?
            AND c.x BETWEEN ? AND ? AND c.y BETWEEN ? AND ? AND c.z BETWEEN ? AND ?
            ORDER BY c.time DESC
            """;
    private static final String SELECT_FILTERED = """
            SELECT c.time, u.name, u.uuid, c.x, c.y, c.z, m.name AS material, c.data, c.amount, c.action
            FROM containers c
            JOIN users u ON c.user_id = u.id
            JOIN levels l ON c.level = l.id
            JOIN materials m ON c.type = m.id
            WHERE l.name = ?
            AND (? IS NULL OR u.name = ?)
            AND (? IS NULL OR c.time >= ?)
            AND (? IS NULL OR c.time <= ?)
            AND (? IS NULL OR c.x BETWEEN ? AND ?)
            AND (? IS NULL OR c.y BETWEEN ? AND ?)
            AND (? IS NULL OR c.z BETWEEN ? AND ?)
            AND (? IS NULL OR c.action = ANY(?))
            AND (? IS NULL OR m.name = ANY(?))
            AND (? IS NULL OR NOT (m.name = ANY(?)))
            ORDER BY c.time DESC LIMIT 1000
            """;

    public void insert(long time, UUID userUuid, String level, int x, int y, int z, SimpleItemStack item, ItemAction action) {
        Dologger.getSqlQueue().enqueue(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, action));
    }

    public void insertList(long time, UUID userUuid, String level, int x, int y, int z, List<SimpleItemStack> items, ItemAction action) {
        List<SqlQueue.SqlTask> tasks = new ArrayList<>();
        for (SimpleItemStack item : items) {
            tasks.add(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, action));
        }
        Dologger.getSqlQueue().enqueueSqlBatch(tasks);
    }

    public void insertMap(long time, UUID userUuid, String level, int x, int y, int z, Map<ItemAction, List<SimpleItemStack>> map) {
        List<SqlQueue.SqlTask> tasks = new ArrayList<>();
        for (Map.Entry<ItemAction, List<SimpleItemStack>> entry : map.entrySet()) {
            for (SimpleItemStack item : entry.getValue()) {
                tasks.add(conn -> insertOne(conn, time, userUuid, level, x, y, z, item, entry.getKey()));
            }
        }
        Dologger.getSqlQueue().enqueueSqlBatch(tasks);
    }

    public List<ContainerHistory> getHistory(String level, int x, int y, int z) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_POSITION)) {
            stmt.setString(1, level);
            stmt.setInt(2, x);
            stmt.setInt(3, y);
            stmt.setInt(4, z);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapHistory(rs);
            }
        }
    }

    public List<ContainerHistory> getHistory(String level, int x1, int y1, int z1, int x2, int y2, int z2) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_RANGE)) {
            stmt.setString(1, level);
            stmt.setInt(2, Math.min(x1, x2));
            stmt.setInt(3, Math.max(x1, x2));
            stmt.setInt(4, Math.min(y1, y2));
            stmt.setInt(5, Math.max(y1, y2));
            stmt.setInt(6, Math.min(z1, z2));
            stmt.setInt(7, Math.max(z1, z2));
            try (ResultSet rs = stmt.executeQuery()) {
                return mapHistory(rs);
            }
        }
    }

    public List<ContainerHistory> getFilteredContainerHistory(String level, List<Object> filters) throws SQLException {
        Connection conn = Dologger.getDatabaseManager().getConnection();
        try (conn; PreparedStatement stmt = conn.prepareStatement(SELECT_FILTERED)) {
            stmt.setString(1, level);
            BlockRepository.bindHistoryFilters(stmt, 2, filters, true);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapHistory(rs);
            }
        }
    }

    static void insertOne(Connection conn, long time, UUID userUuid, String level, int x, int y, int z, SimpleItemStack item, ItemAction action) throws SQLException {
        String material = MaterialRepository.stripMinecraftNamespace(RepositoryMappers.itemName(item));
        try (PreparedStatement upsert = conn.prepareStatement(UPSERT_MATERIAL)) {
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

    private static List<ContainerHistory> mapHistory(ResultSet rs) throws SQLException {
        List<ContainerHistory> history = new ArrayList<>();
        while (rs.next()) {
            history.add(new ContainerHistory(RepositoryMappers.time(rs), RepositoryMappers.user(rs), RepositoryMappers.position(rs), RepositoryMappers.itemStack(rs), ItemAction.fromId(rs.getInt("action"))));
        }
        return history;
    }
}

final class RepositoryMappers {

    private RepositoryMappers() {
    }

    static User user(ResultSet rs) throws SQLException {
        UUID uuid = rs.getObject("uuid", UUID.class);
        return new User(rs.getString("name"), uuid);
    }

    static Time time(ResultSet rs) throws SQLException {
        return new Time(rs.getLong("time"));
    }

    static BlockPosition position(ResultSet rs) throws SQLException {
        return new BlockPosition(rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
    }

    static SimpleItemStack itemStack(ResultSet rs) throws SQLException {
        return new SimpleItemStack(rs.getString("material"), rs.getInt("amount"), deserializeComponents(rs.getBytes("data")));
    }

    static String itemName(SimpleItemStack item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
    }

    static @Nullable DataComponentPatch deserializeComponents(byte @Nullable [] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            CompoundTag tag = NbtIo.readCompressed(inputStream, NbtAccounter.unlimitedHeap());
            var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(ops, tag).getOrThrow(IllegalStateException::new);
            return stack.getComponentsPatch();
        } catch (Exception exception) {
            return null;
        }
    }
}
