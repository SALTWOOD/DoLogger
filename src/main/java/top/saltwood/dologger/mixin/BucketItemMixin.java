package top.saltwood.dologger.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.BlockAction;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @Shadow @Final public Fluid content;

    @Inject(method = "use", at = @At("RETURN"))
    private void dologger$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !cir.getReturnValue().getResult().consumesAction()) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        if (content == Fluids.EMPTY) {
            dologger$logPickup(level, serverPlayer, services);
        } else {
            dologger$logPlace(level, serverPlayer, services);
        }
    }

    @Unique
    private void dologger$logPickup(Level level, ServerPlayer player, Services services) {
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isSource()) {
            return;
        }

        services.block().insertBlock(player, level, pos, state.getBlock(), BlockAction.BREAK_BLOCK);
    }

    @Unique
    private void dologger$logPlace(Level level, ServerPlayer player, Services services) {
        BlockHitResult hit = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        BlockPos placePos = hitState.getBlock() instanceof LiquidBlockContainer && content == Fluids.WATER
                ? hitPos
                : hitPos.relative(hit.getDirection());
        Block placedBlock = content.defaultFluidState().createLegacyBlock().getBlock();
        services.block().insertBlock(player, level, placePos, placedBlock, BlockAction.PLACE_BLOCK);
    }
}
