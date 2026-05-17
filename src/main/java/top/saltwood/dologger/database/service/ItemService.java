package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.saltwood.dologger.database.repository.ItemRepository;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.ItemHistory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemService {

    private final ItemRepository itemRepository = new ItemRepository();
    private final UserService userService;
    private final LevelService levelService;
    private final MaterialService materialService;

    ItemService(UserService userService, LevelService levelService, MaterialService materialService) {
        this.userService = userService;
        this.levelService = levelService;
        this.materialService = materialService;
    }

    public boolean insert(ServerPlayer player, Level level, BlockPos pos, ItemStack item, ItemAction action) {
        return insert(player.getUUID(), player.getGameProfile().getName(), level, pos, SimpleItemStack.of(item), action);
    }

    public boolean insert(UUID userUuid, String username, Level level, BlockPos pos, SimpleItemStack item, ItemAction action) {
        if (item == null || item.isEmpty() || !prepare(userUuid, username, level, List.of(item))) {
            return false;
        }
        return itemRepository.insert(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), item, action);
    }

    public boolean insertList(UUID userUuid, String username, Level level, BlockPos pos, List<SimpleItemStack> items, ItemAction action) {
        List<SimpleItemStack> filtered = items == null ? List.of() : items.stream().filter(item -> item != null && !item.isEmpty()).toList();
        if (filtered.isEmpty() || !prepare(userUuid, username, level, filtered)) {
            return false;
        }
        return itemRepository.insertList(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), filtered, action);
    }

    public boolean insertMap(UUID userUuid, String username, Level level, BlockPos pos, Map<ItemAction, List<SimpleItemStack>> map) {
        if (map == null || map.isEmpty() || !userService.ensure(userUuid, username) || !levelService.ensure(level)) {
            return false;
        }
        map.values().forEach(items -> items.forEach(item -> materialService.ensure(ServiceSupport.itemName(item.toItemStack()))));
        return itemRepository.insertMap(System.currentTimeMillis(), userUuid, ServiceSupport.levelName(level), pos.getX(), pos.getY(), pos.getZ(), map);
    }

    public List<ItemHistory> getFilteredItemHistory(Level level, List<Object> filters) {
        if (!ServiceSupport.canRead()) {
            ServiceSupport.logUnavailable();
            return List.of();
        }
        try {
            return itemRepository.getFilteredItemHistory(ServiceSupport.levelName(level), filters);
        } catch (SQLException exception) {
            ServiceSupport.logSqlFailure(exception);
            return List.of();
        }
    }

    private boolean prepare(UUID userUuid, String username, Level level, List<SimpleItemStack> items) {
        if (!userService.ensure(userUuid, username) || !levelService.ensure(level)) {
            return false;
        }
        items.forEach(item -> materialService.ensure(ServiceSupport.itemName(item.toItemStack())));
        return true;
    }
}
