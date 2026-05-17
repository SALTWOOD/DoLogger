package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import top.saltwood.dologger.database.repository.CommandRepository;
import top.saltwood.dologger.model.history.CommandHistory;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class CommandService {

    private final CommandRepository commandRepository = new CommandRepository();
    private final UserService userService;
    private final LevelService levelService;

    CommandService(UserService userService, LevelService levelService) {
        this.userService = userService;
        this.levelService = levelService;
    }

    public boolean insert(ServerPlayer player, String command) {
        return insert(player.getUUID(), player.getGameProfile().getName(), player.level(), ServiceSupport.blockPosition(player), command);
    }

    public boolean insert(UUID userUuid, String username, Level level, BlockPos pos, String command) {
        if (command == null || !userService.ensure(userUuid, username) || !levelService.ensure(level)) {
            return false;
        }
        return commandRepository.insert(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), command);
    }

    public List<CommandHistory> getFilteredCommandHistory(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return commandRepository.getFilteredCommandHistory(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }
}
