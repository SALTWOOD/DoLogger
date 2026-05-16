package top.saltwood.dologger.block;

import net.minecraft.world.item.ItemStack;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerTransactionManager {

    private final Map<SimpleItemStack, Integer> snapshot;

    public ContainerTransactionManager(List<ItemStack> snapshotItems) {
        this.snapshot = countItems(snapshotItems);
    }

    public Map<ItemAction, List<SimpleItemStack>> finalize(List<ItemStack> currentItems) {
        Map<SimpleItemStack, Integer> current = countItems(currentItems);
        Map<ItemAction, List<SimpleItemStack>> diff = new EnumMap<>(ItemAction.class);

        for (Map.Entry<SimpleItemStack, Integer> entry : current.entrySet()) {
            SimpleItemStack item = entry.getKey();
            int delta = entry.getValue() - snapshot.getOrDefault(item, 0);
            addDelta(diff, item, delta);
        }

        for (Map.Entry<SimpleItemStack, Integer> entry : snapshot.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                addDelta(diff, entry.getKey(), -entry.getValue());
            }
        }

        return diff;
    }

    protected static void merge(Map<ItemAction, List<SimpleItemStack>> target, Map<ItemAction, List<SimpleItemStack>> source) {
        source.forEach((action, items) -> target.computeIfAbsent(action, ignored -> new ArrayList<>()).addAll(items));
    }

    private static Map<SimpleItemStack, Integer> countItems(List<ItemStack> items) {
        Map<SimpleItemStack, Integer> counts = new HashMap<>();
        if (items == null) {
            return counts;
        }

        for (ItemStack stack : items) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            SimpleItemStack simple = SimpleItemStack.of(stack);
            counts.merge(simple, stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static void addDelta(Map<ItemAction, List<SimpleItemStack>> diff, SimpleItemStack item, int delta) {
        if (delta == 0 || item == null || item.isEmpty()) {
            return;
        }

        ItemAction action = delta > 0 ? ItemAction.ADD_ITEM : ItemAction.REMOVE_ITEM;
        SimpleItemStack changed = new SimpleItemStack(item.getItem(), Math.abs(delta), item.getComponents());
        diff.computeIfAbsent(action, ignored -> new ArrayList<>()).add(changed);
    }
}
