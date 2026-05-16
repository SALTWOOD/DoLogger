package top.saltwood.dologger.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.event.item.ItemQueue;
import top.saltwood.dologger.model.action.ItemAction;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void dologger$logConsume(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (Dologger.getServices() == null || level.isClientSide() || !(entity instanceof ServerPlayer player) || stack.isEmpty()) {
            return;
        }

        ItemQueue.queue(player, ItemAction.CONSUME_ITEM, stack.copyWithCount(1));
    }
}
