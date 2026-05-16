package top.saltwood.dologger.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.util.EntityUtils;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EnderManTakeBlockGoalMixin {

    @Shadow @Final private EnderMan enderman;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean dologger$removeBlock(Level level, BlockPos pos, boolean moving) {
        EntityUtils.logBlockAction(enderman, level, pos, level.getBlockState(pos).getBlock(), BlockAction.BREAK_BLOCK);
        return level.removeBlock(pos, moving);
    }
}
