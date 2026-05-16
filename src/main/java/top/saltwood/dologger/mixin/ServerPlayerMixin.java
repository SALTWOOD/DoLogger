package top.saltwood.dologger.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.block.ContainerHandler;
import top.saltwood.dologger.block.ContainerTransactionManager;
import top.saltwood.dologger.block.ContainersTransactionManager;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.event.BlockEvents;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.IHistory;
import top.saltwood.dologger.player.DoLoggerServerPlayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements DoLoggerServerPlayer {

    @Unique private boolean dologger$inspecting = false;
    @Unique private ContainerTransactionManager dologger$containerTransactionManager;
    @Unique private Map<ItemAction, List<SimpleItemStack>> dologger$itemQueue = new HashMap<>();
    @Unique private List<List<IHistory>> dologger$pages;
    @Unique private Level dologger$containerLevel;
    @Unique private BlockPos dologger$containerPos;

    @Override
    public boolean dologger$isInspecting() {
        return dologger$inspecting;
    }

    @Override
    public void dologger$setInspecting(boolean inspecting) {
        dologger$inspecting = inspecting;
    }

    @Override
    public ContainerTransactionManager dologger$getContainerTransactionManager() {
        return dologger$containerTransactionManager;
    }

    @Override
    public void dologger$setContainerTransactionManager(ContainerTransactionManager manager) {
        dologger$containerTransactionManager = manager;
    }

    @Override
    public Map<ItemAction, List<SimpleItemStack>> dologger$getItemQueue() {
        return dologger$itemQueue;
    }

    @Override
    public void dologger$setItemQueue(Map<ItemAction, List<SimpleItemStack>> queue) {
        dologger$itemQueue = queue == null ? new HashMap<>() : queue;
    }

    @Override
    public List<List<IHistory>> dologger$getPages() {
        return dologger$pages;
    }

    @Override
    public void dologger$setPages(List<List<IHistory>> pages) {
        dologger$pages = pages;
    }

    @Inject(method = "openMenu", at = @At("HEAD"))
    private void dologger$openMenu(MenuProvider menuProvider, CallbackInfoReturnable<OptionalInt> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().isClientSide() || BlockEvents.isInspecting(player)) {
            return;
        }

        dologger$containerTransactionManager = null;
        dologger$containerLevel = null;
        dologger$containerPos = null;

        if (!(menuProvider instanceof Container container)) {
            return;
        }

        BlockPos pos = dologger$findContainerPos(player.level(), container, player.blockPosition());
        if (pos == null) {
            return;
        }

        ContainersTransactionManager manager = new ContainersTransactionManager();
        dologger$addContainer(manager, player.level(), pos);
        dologger$addContainer(manager, player.level(), BlockEvents.chestPartnerPosition(player.level(), pos));

        if (manager.primaryPos() == null) {
            return;
        }

        dologger$containerTransactionManager = manager;
        dologger$containerLevel = player.level();
        dologger$containerPos = manager.primaryPos();
    }

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void dologger$doCloseContainer(CallbackInfo ci) {
        if (!(dologger$containerTransactionManager instanceof ContainersTransactionManager manager)
                || dologger$containerLevel == null
                || dologger$containerPos == null) {
            return;
        }

        Map<BlockPos, Container> currentContainers = new HashMap<>();
        for (BlockPos pos : manager.positions()) {
            Container container = ContainerHandler.getContainer(dologger$containerLevel, pos);
            if (container != null) {
                currentContainers.put(pos, container);
            }
        }

        Map<ItemAction, List<SimpleItemStack>> diff = manager.finalizeAll(currentContainers);
        if (!diff.isEmpty()) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            Services services = Dologger.getServices();
            if (services != null) {
                services.container().insertMap(player.getUUID(), player.getGameProfile().getName(), dologger$containerLevel, dologger$containerPos, diff);
            }
        }

        dologger$containerTransactionManager = null;
        dologger$containerLevel = null;
        dologger$containerPos = null;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dologger$tick(CallbackInfo ci) {
        if (dologger$itemQueue.isEmpty()) {
            return;
        }

        ServerPlayer player = (ServerPlayer) (Object) this;
        Services services = Dologger.getServices();
        if (services != null) {
            services.item().insertMap(player.getUUID(), player.getGameProfile().getName(), player.level(), player.blockPosition(), dologger$itemQueue);
        }
        dologger$itemQueue = new HashMap<>();
    }

    @Inject(method = "drop", at = @At("RETURN"))
    private void dologger$drop(ItemStack droppedItem, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
        if (droppedItem == null || droppedItem.isEmpty() || cir.getReturnValue() == null) {
            return;
        }
        dologger$itemQueue.computeIfAbsent(ItemAction.DROP_ITEM, ignored -> new java.util.ArrayList<>()).add(SimpleItemStack.of(droppedItem.copy()));
    }

    @Unique
    private static void dologger$addContainer(ContainersTransactionManager manager, Level level, BlockPos pos) {
        Container container = ContainerHandler.getContainer(level, pos);
        if (container != null) {
            manager.add(pos, container);
        }
    }

    @Unique
    private static BlockPos dologger$findContainerPos(Level level, Container target, BlockPos center) {
        int radius = 6;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (ContainerHandler.getContainer(level, pos) == target) {
                return pos.immutable();
            }
        }
        return null;
    }
}
