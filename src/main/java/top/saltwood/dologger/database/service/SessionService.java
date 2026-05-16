package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import top.saltwood.dologger.database.repository.SessionRepository;
import top.saltwood.dologger.model.action.SessionAction;
import top.saltwood.dologger.model.history.SessionHistory;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class SessionService {

    private final SessionRepository sessionRepository = new SessionRepository();
    private final UserService userService;
    private final LevelService levelService;

    SessionService(UserService userService, LevelService levelService) {
        this.userService = userService;
        this.levelService = levelService;
    }

    public boolean insert(ServerPlayer player, SessionAction action) {
        return insert(player.getUUID(), player.getGameProfile().getName(), player.level(), ServiceSupport.blockPosition(player), action);
    }

    public boolean insert(UUID userUuid, String username, Level level, BlockPos pos, SessionAction action) {
        if (!userService.ensure(userUuid, username) || !levelService.ensure(level)) {
            return false;
        }
        sessionRepository.insert(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), action);
        return true;
    }

    public List<SessionHistory> getFilteredSessionHistory(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return sessionRepository.getFilteredSessionHistory(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }
}
