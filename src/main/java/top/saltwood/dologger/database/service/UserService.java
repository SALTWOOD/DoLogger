package top.saltwood.dologger.database.service;

import net.minecraft.server.level.ServerPlayer;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.repository.UserRepository;
import top.saltwood.dologger.database.repository.UsernameRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserService {

    private static final String SELECT_ID_BY_UUID = "SELECT id FROM users WHERE uuid = ?";

    private final UserRepository userRepository = new UserRepository();
    private final UsernameRepository usernameRepository = new UsernameRepository();
    private final Map<UUID, Integer> userIds = new ConcurrentHashMap<>();

    public boolean ensure(ServerPlayer player) {
        return ensure(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean ensure(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank() || !ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        userRepository.insertOrUpdateName(name, uuid);
        usernameRepository.insert(System.currentTimeMillis(), uuid, name);
        userIds.remove(uuid);
        return true;
    }

    public boolean insertNonPlayer(String name) {
        if (name == null || name.isBlank() || !ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        userRepository.insertNonPlayer(name);
        return true;
    }

    public Integer getId(UUID uuid) {
        if (uuid == null || !ServiceSupport.canRead()) {
            return null;
        }
        Integer cached = userIds.get(uuid);
        if (cached != null) {
            return cached;
        }
        try (var connection = Dologger.getDatabaseManager().getConnection(); PreparedStatement stmt = connection.prepareStatement(SELECT_ID_BY_UUID)) {
            stmt.setObject(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    userIds.put(uuid, id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
        }
        return null;
    }

    public Map<Integer, String> getAllUsernames() {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return Collections.emptyMap();
        }
        try {
            return userRepository.getAllUsernames();
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return Collections.emptyMap();
        }
    }

    public void clearCache() {
        userIds.clear();
    }
}
