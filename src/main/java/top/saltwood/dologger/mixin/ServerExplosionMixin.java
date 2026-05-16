package top.saltwood.dologger.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.util.EntityUtils;

@Mixin(Explosion.class)
public abstract class ServerExplosionMixin {

    @Shadow @Final private Level level;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void dologger$finalizeExplosion(boolean spawnParticles, CallbackInfo ci) {
        if (level.isClientSide()) {
            return;
        }

        Explosion explosion = (Explosion) (Object) this;
        Entity source = explosion.getIndirectSourceEntity();
        if (source == null) {
            source = explosion.getDirectSourceEntity();
        }

        for (BlockPos pos : toBlow) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                EntityUtils.logBlockAction(source, level, pos, state.getBlock(), BlockAction.BREAK_BLOCK);
            }
        }
    }
}
