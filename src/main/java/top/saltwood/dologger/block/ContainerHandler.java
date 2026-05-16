package top.saltwood.dologger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public final class ContainerHandler {

    private ContainerHandler() {
    }

    public static Container getContainer(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof Container container ? container : null;
    }

    public static List<ItemStack> getContainerItems(Container container) {
        if (container == null) {
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
}
