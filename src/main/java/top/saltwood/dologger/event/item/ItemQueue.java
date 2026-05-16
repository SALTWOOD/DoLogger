package top.saltwood.dologger.event.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemQueue {

    private static final Map<UUID, Map<ItemAction, List<SimpleItemStack>>> QUEUES = new java.util.concurrent.ConcurrentHashMap<>();

    private ItemQueue() {
    }

    public static void queue(ServerPlayer player, ItemAction action, ItemStack stack) {
        if (player == null || player.level().isClientSide() || action == null || stack == null || stack.isEmpty()) {
            return;
        }

        queue(player, action, SimpleItemStack.of(stack));
    }

    public static void queue(ServerPlayer player, ItemAction action, SimpleItemStack stack) {
        if (player == null || player.level().isClientSide() || action == null || stack == null || stack.isEmpty()) {
            return;
        }

        Map<ItemAction, List<SimpleItemStack>> playerQueue = QUEUES.computeIfAbsent(player.getUUID(), ignored -> new EnumMap<>(ItemAction.class));
        List<SimpleItemStack> items = playerQueue.computeIfAbsent(action, ignored -> new ArrayList<>());
        for (int index = 0; index < items.size(); index++) {
            SimpleItemStack existing = items.get(index);
            if (existing.equals(stack)) {
                items.set(index, new SimpleItemStack(existing.getItem(), existing.getCount() + stack.getCount(), existing.getComponents()));
                return;
            }
        }
        items.add(stack);
    }

    public static void flush(ServerPlayer player) {
        Services services = Dologger.getServices();
        if (player == null || services == null || player.level().isClientSide()) {
            return;
        }

        Map<ItemAction, List<SimpleItemStack>> queued = QUEUES.remove(player.getUUID());
        if (queued == null || queued.isEmpty()) {
            return;
        }

        queued.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        if (queued.isEmpty()) {
            return;
        }

        BlockPos pos = player.blockPosition();
        services.item().insertMap(player.getUUID(), player.getGameProfile().getName(), player.level(), pos, queued);
    }

    public static void flushAll(Iterable<ServerPlayer> players) {
        if (players == null) {
            return;
        }

        for (ServerPlayer player : players) {
            flush(player);
        }
    }

    public static void remove(UUID playerId) {
        if (playerId != null) {
            QUEUES.remove(playerId);
        }
    }

    public static void removeMissingPlayers(Iterable<ServerPlayer> players) {
        List<UUID> online = new ArrayList<>();
        if (players != null) {
            for (ServerPlayer player : players) {
                online.add(player.getUUID());
            }
        }

        Iterator<UUID> iterator = QUEUES.keySet().iterator();
        while (iterator.hasNext()) {
            if (!online.contains(iterator.next())) {
                iterator.remove();
            }
        }
    }
}
