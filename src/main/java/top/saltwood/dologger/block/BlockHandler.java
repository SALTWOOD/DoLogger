package top.saltwood.dologger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BlockHandler {

    private static final Set<ResourceLocation> INTERACTABLE_BLOCKS = Set.of(
            blockId("anvil"),
            blockId("chipped_anvil"),
            blockId("damaged_anvil"),
            blockId("beacon"),
            blockId("blast_furnace"),
            blockId("brewing_stand"),
            blockId("crafting_table"),
            blockId("furnace"),
            blockId("grindstone"),
            blockId("lectern"),
            blockId("loom"),
            blockId("smithing_table"),
            blockId("smoker"),
            blockId("stonecutter"),
            blockId("cartography_table"),
            blockId("enchanting_table"),
            blockId("bed"),
            blockId("cake"),
            blockId("comparator"),
            blockId("jukebox"),
            blockId("note_block"),
            blockId("repeater"),
            blockId("trapped_chest"),
            blockId("chest"),
            blockId("barrel"),
            blockId("hopper"),
            blockId("dispenser"),
            blockId("dropper")
    );

    private static final Set<ResourceLocation> CONTAINER_BLOCKS = Set.of(
            blockId("chest"),
            blockId("trapped_chest"),
            blockId("barrel"),
            blockId("hopper"),
            blockId("dispenser"),
            blockId("dropper"),
            blockId("furnace"),
            blockId("blast_furnace"),
            blockId("smoker"),
            blockId("brewing_stand")
    );

    private BlockHandler() {
    }

    public static boolean isInteractable(Block block) {
        return INTERACTABLE_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static BlockPos getSecondDoorPosition(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof DoorBlock) || !state.hasProperty(DoorBlock.HALF)) {
            return null;
        }
        return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
    }

    public static BlockPos getChestPartnerPosition(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE) || !state.hasProperty(ChestBlock.FACING)) {
            return null;
        }

        ChestType chestType = state.getValue(ChestBlock.TYPE);
        if (chestType == ChestType.SINGLE) {
            return null;
        }

        Direction facing = state.getValue(ChestBlock.FACING);
        Direction partnerDirection = chestType == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos partnerPos = pos.relative(partnerDirection);
        BlockState partnerState = level.getBlockState(partnerPos);

        return partnerState.getBlock() == state.getBlock() ? partnerPos : null;
    }

    public static boolean isContainer(Block block) {
        return CONTAINER_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(block));
    }

    public static List<ItemStack> getContainerItems(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof Container container)) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack item = container.getItem(slot);
            if (!item.isEmpty()) {
                items.add(item.copy());
            }
        }
        return items;
    }

    private static ResourceLocation blockId(String path) {
        return ResourceLocation.withDefaultNamespace(path);
    }
}
