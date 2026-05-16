package top.saltwood.dologger.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.event.item.ItemQueue;
import top.saltwood.dologger.model.action.ItemAction;

@Mixin(Projectile.class)
public class ProjectileMixin {

    @Inject(method = "shootFromRotation", at = @At("HEAD"))
    private void dologger$logProjectileUse(Entity shooter, float x, float y, float z, float velocity, float inaccuracy, CallbackInfo ci) {
        if (Dologger.getServices() == null || !(shooter instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }

        Projectile projectile = (Projectile) (Object) this;
        if (projectile instanceof AbstractArrow arrow) {
            ItemQueue.queue(player, ItemAction.SHOOT_ITEM, arrowItem(arrow));
        } else if (projectile instanceof ThrowableProjectile) {
            ItemQueue.queue(player, ItemAction.THROW_ITEM, projectileItem(projectile));
        }
    }

    private static ItemStack projectileItem(Projectile projectile) {
        if (projectile instanceof ItemSupplier itemSupplier) {
            ItemStack stack = itemSupplier.getItem();
            if (!stack.isEmpty()) {
                return stack.copyWithCount(1);
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack arrowItem(AbstractArrow arrow) {
        ItemStack pickupItem = arrow.getPickupItemStackOrigin();
        if (!pickupItem.isEmpty()) {
            return pickupItem.copyWithCount(1);
        }
        if (arrow instanceof SpectralArrow) {
            return new ItemStack(Items.SPECTRAL_ARROW);
        }
        if (arrow instanceof ThrownTrident) {
            return new ItemStack(Items.TRIDENT);
        }
        if (arrow instanceof Arrow) {
            return new ItemStack(Items.ARROW);
        }
        return new ItemStack(Items.ARROW);
    }
}
