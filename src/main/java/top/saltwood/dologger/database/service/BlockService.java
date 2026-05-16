package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import top.saltwood.dologger.database.repository.BlockRepository;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.model.history.BlockHistory;
import top.saltwood.dologger.model.history.IHistory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BlockService {

    private final BlockRepository blockRepository = new BlockRepository();
    private final UserService userService;
    private final LevelService levelService;
    private final MaterialService materialService;
    private final EntityService entityService;

    BlockService(UserService userService, LevelService levelService, MaterialService materialService, EntityService entityService) {
        this.userService = userService;
        this.levelService = levelService;
        this.materialService = materialService;
        this.entityService = entityService;
    }

    public boolean insertBlock(ServerPlayer player, Level level, BlockPos pos, Block block, BlockAction action) {
        return insertBlock(player.getUUID(), player.getGameProfile().getName(), level, pos, BuiltInRegistries.BLOCK.getKey(block).toString(), action);
    }

    public boolean insertBlock(UUID userUuid, String username, Level level, BlockPos pos, String material, BlockAction action) {
        if (!prepareUserLevel(userUuid, username, level) || !materialService.ensure(material)) {
            return false;
        }
        blockRepository.insertMaterial(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), material, action);
        return true;
    }

    public boolean insertEntity(ServerPlayer player, Level level, BlockPos pos, Entity entity, BlockAction action) {
        return insertEntity(player.getUUID(), player.getGameProfile().getName(), level, pos, ServiceSupport.entityName(entity), action);
    }

    public boolean insertEntity(UUID userUuid, String username, Level level, BlockPos pos, Entity entity, BlockAction action) {
        return insertEntity(userUuid, username, level, pos, ServiceSupport.entityName(entity), action);
    }

    public boolean insertEntity(UUID userUuid, String username, Level level, BlockPos pos, String entity, BlockAction action) {
        if (!prepareUserLevel(userUuid, username, level) || !entityService.ensure(entity)) {
            return false;
        }
        blockRepository.insertEntity(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), entity, action);
        return true;
    }

    public List<BlockHistory> getBlockHistory(Level level, BlockPos pos) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return blockRepository.getBlockHistory(ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ());
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    public List<IHistory> getBlockHistory(Level level, List<BlockPos> positions) {
        List<IHistory> history = new ArrayList<>();
        for (BlockPos pos : positions) {
            history.addAll(getBlockHistory(level, pos));
        }
        return sorted(history);
    }

    public List<BlockHistory> getInteractionHistory(Level level, BlockPos pos) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return blockRepository.getInteractionHistory(ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ());
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    public List<IHistory> getInteractionHistory(Level level, List<BlockPos> positions) {
        List<IHistory> history = new ArrayList<>();
        for (BlockPos pos : positions) {
            history.addAll(getInteractionHistory(level, pos));
        }
        return sorted(history);
    }

    public List<BlockHistory> getFilteredBlockHistory(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return blockRepository.getFilteredBlockHistory(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    public List<BlockHistory> getRevertCandidates(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return blockRepository.getRevertCandidates(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    public List<BlockHistory> getRestoreCandidates(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return blockRepository.getRestoreCandidates(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    public boolean markReverted(int id, UUID actorUuid, long at, UUID batch) {
        if (!ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        try {
            return blockRepository.markReverted(id, actorUuid, at, batch);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return false;
        }
    }

    public boolean markRestored(int id, UUID actorUuid, long at, UUID batch) {
        if (!ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        try {
            return blockRepository.markRestored(id, actorUuid, at, batch);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return false;
        }
    }

    public boolean insertGeneratedBlock(ServerPlayer player, Level level, BlockPos pos, Block block, BlockAction action, int sourceBlockId) {
        return insertGeneratedBlock(player.getUUID(), player.getGameProfile().getName(), level, pos, BuiltInRegistries.BLOCK.getKey(block).toString(), action, sourceBlockId);
    }

    public boolean insertGeneratedBlock(UUID userUuid, String username, Level level, BlockPos pos, String material, BlockAction action, int sourceBlockId) {
        if (!prepareUserLevel(userUuid, username, level) || !materialService.ensure(material)) {
            return false;
        }
        blockRepository.insertGeneratedMaterial(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), material, action, sourceBlockId);
        return true;
    }

    public boolean removeInteractions(Level level, BlockPos pos) {
        if (!ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        blockRepository.removeInteractionsForPosition(ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ());
        return true;
    }

    private boolean prepareUserLevel(UUID userUuid, String username, Level level) {
        return userService.ensure(userUuid, username) && levelService.ensure(level);
    }

    private static List<IHistory> sorted(List<IHistory> history) {
        return history.stream().sorted(Comparator.comparingLong((IHistory item) -> item.getTime().time()).reversed()).toList();
    }
}
