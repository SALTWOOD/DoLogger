package top.saltwood.dologger.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContainersTransactionManager extends ContainerTransactionManager {

    private final Map<BlockPos, ContainerTransactionManager> transactions = new LinkedHashMap<>();

    public ContainersTransactionManager() {
        super(List.of());
    }

    public void add(BlockPos pos, Container container) {
        if (pos == null || container == null) {
            return;
        }
        transactions.putIfAbsent(pos.immutable(), new ContainerTransactionManager(ContainerHandler.getContainerItems(container)));
    }

    public Map<ItemAction, List<SimpleItemStack>> finalizeAll(Map<BlockPos, Container> currentContainers) {
        Map<ItemAction, List<SimpleItemStack>> combined = new EnumMap<>(ItemAction.class);
        transactions.forEach((pos, transaction) -> {
            Container current = currentContainers.get(pos);
            if (current != null) {
                merge(combined, transaction.finalize(ContainerHandler.getContainerItems(current)));
            }
        });
        return combined;
    }

    public BlockPos primaryPos() {
        return transactions.keySet().stream().findFirst().orElse(null);
    }

    public List<BlockPos> positions() {
        return List.copyOf(transactions.keySet());
    }
}
