package top.saltwood.dologger.player;

import top.saltwood.dologger.block.ContainerTransactionManager;
import top.saltwood.dologger.model.SimpleItemStack;
import top.saltwood.dologger.model.action.ItemAction;
import top.saltwood.dologger.model.history.IHistory;

import java.util.List;
import java.util.Map;

public interface DoLoggerServerPlayer {
    boolean dologger$isInspecting();

    void dologger$setInspecting(boolean inspecting);

    ContainerTransactionManager dologger$getContainerTransactionManager();

    void dologger$setContainerTransactionManager(ContainerTransactionManager manager);

    Map<ItemAction, List<SimpleItemStack>> dologger$getItemQueue();

    void dologger$setItemQueue(Map<ItemAction, List<SimpleItemStack>> queue);

    List<List<IHistory>> dologger$getPages();

    void dologger$setPages(List<List<IHistory>> pages);
}
