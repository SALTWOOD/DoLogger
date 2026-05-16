package top.saltwood.dologger.event;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.block.BlockHandler;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.IHistory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BlockEvents {

    private static final Set<UUID> inspectingPlayers = new HashSet<>();

    public static boolean isInspecting(ServerPlayer player) {
        return inspectingPlayers.contains(player.getUUID());
    }

    public static void toggleInspect(ServerPlayer player) {
        if (!inspectingPlayers.remove(player.getUUID())) {
            inspectingPlayers.add(player.getUUID());
            player.sendSystemMessage(Component.literal("DoLogger inspect enabled").withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.literal("DoLogger inspect disabled").withStyle(ChatFormatting.RED));
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = event.getLevel() instanceof Level eventLevel ? eventLevel : null;
        if (level == null || level.isClientSide() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = event.getState();

        if (isInspecting(player)) {
            showHistory(player, services, level, historyPositions(level, pos, state));
            event.setCanceled(true);
            return;
        }

        services.block().removeInteractions(level, pos);
        Block block = state.getBlock();
        if (BlockHandler.isContainer(block)) {
            for (ItemStack itemStack : BlockHandler.getContainerItems(level, pos)) {
                services.container().insert(player, level, pos, itemStack, ItemAction.REMOVE_ITEM);
            }
        }
        services.block().insertBlock(player, level, pos, block, BlockAction.BREAK_BLOCK);
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        Level level = event.getLevel() instanceof Level eventLevel ? eventLevel : null;
        if (level == null || level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        BlockState state = event.getPlacedBlock();
        services.block().insertBlock(player, level, event.getPos(), state.getBlock(), BlockAction.PLACE_BLOCK);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (isInspecting(player)) {
            showHistory(player, services, level, historyPositions(level, pos, state));
            event.setCanceled(true);
            return;
        }

        Block block = state.getBlock();
        if (BlockHandler.isInteractable(block)) {
            services.block().insertBlock(player, level, pos, block, BlockAction.INTERACT_BLOCK);
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(event.getEntity() instanceof ServerPlayer player) || !isInspecting(player)) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        BlockPos pos = event.getPos();
        showHistory(player, services, level, historyPositions(level, pos, level.getBlockState(pos)));
        event.setCanceled(true);
    }

    private static List<BlockPos> historyPositions(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(pos);

        BlockPos secondDoorPos = BlockHandler.getSecondDoorPosition(level, pos, state);
        if (secondDoorPos != null) {
            positions.add(secondDoorPos);
        }

        BlockPos chestPartnerPos = BlockHandler.getChestPartnerPosition(level, pos, state);
        if (chestPartnerPos != null) {
            positions.add(chestPartnerPos);
        }

        return positions;
    }

    private static void showHistory(ServerPlayer player, Services services, Level level, List<BlockPos> positions) {
        List<IHistory> history = new ArrayList<>();
        history.addAll(services.block().getBlockHistory(level, positions));
        history.addAll(services.block().getInteractionHistory(level, positions));
        if (positions.size() > 1) {
            history.addAll(services.container().getHistory(level, positions.get(0), positions.get(1)));
        } else {
            history.addAll(services.container().getHistory(level, positions.getFirst()));
        }

        if (history.isEmpty()) {
            player.sendSystemMessage(Component.literal("No history found for this block.").withStyle(ChatFormatting.GRAY));
            return;
        }

        history.stream()
                .sorted(Comparator.comparingLong((IHistory entry) -> entry.getTime().time()).reversed())
                .limit(10)
                .map(IHistory::getComponentWithPos)
                .forEach(player::sendSystemMessage);
    }
}
