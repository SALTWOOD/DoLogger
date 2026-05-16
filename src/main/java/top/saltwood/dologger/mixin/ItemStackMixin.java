package top.saltwood.dologger.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.event.item.ItemQueue;
import top.saltwood.dologger.model.action.ItemAction;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerPlayer;Ljava/util/function/Consumer;)V", at = @At("HEAD"))
    private void dologger$logServerPlayerBreak(int damage, ServerLevel level, ServerPlayer player, Consumer<?> onBroken, CallbackInfo ci) {
        dologger$queueBreak(player, damage);
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At("HEAD"))
    private void dologger$logLivingEntityBreak(int damage, ServerLevel level, LivingEntity entity, Consumer<?> onBroken, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            dologger$queueBreak(player, damage);
        }
    }

    @Inject(method = "hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V", at = @At("HEAD"))
    private void dologger$logEquipmentBreak(int damage, LivingEntity entity, EquipmentSlot slot, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player) {
            dologger$queueBreak(player, damage);
        }
    }

    private void dologger$queueBreak(ServerPlayer player, int damage) {
        if (Dologger.getServices() == null || player == null || player.level().isClientSide()) {
            return;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (!stack.isDamageableItem() || stack.getDamageValue() + damage < stack.getMaxDamage()) {
            return;
        }

        ItemQueue.queue(player, ItemAction.BREAK_ITEM, stack.copyWithCount(1));
    }
}
