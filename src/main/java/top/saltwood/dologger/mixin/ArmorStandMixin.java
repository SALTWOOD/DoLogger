package top.saltwood.dologger.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.BlockAction;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin {

    @Inject(method = "interactAt", at = @At("RETURN"))
    private void dologger$interactAt(Player player, Vec3 vec, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!(player instanceof ServerPlayer serverPlayer) || cir.getReturnValue() != InteractionResult.SUCCESS) {
            return;
        }

        ArmorStand armorStand = (ArmorStand) (Object) this;
        if (armorStand.level().isClientSide()) {
            return;
        }

        Services services = Dologger.getServices();
        if (services != null) {
            services.block().insertEntity(serverPlayer, armorStand.level(), armorStand.blockPosition(), armorStand, BlockAction.INTERACT_ENTITY);
        }
    }
}
