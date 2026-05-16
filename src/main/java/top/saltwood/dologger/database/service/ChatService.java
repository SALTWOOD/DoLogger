package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import top.saltwood.dologger.database.repository.ChatRepository;

import java.util.UUID;

public class ChatService {

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserService userService;
    private final LevelService levelService;

    ChatService(UserService userService, LevelService levelService) {
        this.userService = userService;
        this.levelService = levelService;
    }

    public boolean insert(ServerPlayer player, String message) {
        return insert(player.getUUID(), player.getGameProfile().getName(), player.level(), ServiceSupport.blockPosition(player), message);
    }

    public boolean insert(UUID userUuid, String username, Level level, BlockPos pos, String message) {
        if (message == null || !userService.ensure(userUuid, username) || !levelService.ensure(level)) {
            return false;
        }
        chatRepository.insert(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), message);
        return true;
    }
}
