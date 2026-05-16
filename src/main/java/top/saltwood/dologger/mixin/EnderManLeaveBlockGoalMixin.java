package top.saltwood.dologger.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.util.EntityUtils;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EnderManLeaveBlockGoalMixin {

    @Shadow @Final private EnderMan enderman;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean dologger$setBlock(Level level, BlockPos pos, BlockState state, int flags) {
        boolean placed = level.setBlock(pos, state, flags);
        if (placed) {
            EntityUtils.logBlockAction(enderman, level, pos, state.getBlock(), BlockAction.PLACE_BLOCK);
        }
        return placed;
    }
}
