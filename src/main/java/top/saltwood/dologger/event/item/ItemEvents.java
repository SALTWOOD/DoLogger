package top.saltwood.dologger.event.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.model.action.ItemAction;

public class ItemEvents {

    @SubscribeEvent
    public void onCraftItem(PlayerEvent.ItemCraftedEvent event) {
        if (Dologger.getServices() == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemQueue.queue(player, ItemAction.CRAFT_ITEM, event.getCrafting());
    }

    @SubscribeEvent
    public void onSmeltItem(PlayerEvent.ItemSmeltedEvent event) {
        if (Dologger.getServices() == null || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ItemQueue.queue(player, ItemAction.CRAFT_ITEM, event.getSmelting());
    }

    @SubscribeEvent
    public void onPickupItem(ItemEntityPickupEvent.Post event) {
        if (Dologger.getServices() == null || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ItemStack pickedUp = event.getOriginalStack();
        pickedUp.shrink(event.getCurrentStack().getCount());
        ItemQueue.queue(player, ItemAction.PICKUP_ITEM, pickedUp);
    }

    @SubscribeEvent
    public void onDropItem(ItemTossEvent event) {
        if (Dologger.getServices() == null || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        ItemEntity entity = event.getEntity();
        ItemQueue.queue(player, ItemAction.DROP_ITEM, entity.getItem().copy());
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (Dologger.getServices() == null) {
            return;
        }

        ItemQueue.flushAll(event.getServer().getPlayerList().getPlayers());
        ItemQueue.removeMissingPlayers(event.getServer().getPlayerList().getPlayers());
    }
}
