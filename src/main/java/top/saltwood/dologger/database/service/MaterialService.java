package top.saltwood.dologger.database.service;

import net.minecraft.world.item.ItemStack;
import top.saltwood.dologger.database.repository.MaterialRepository;

public class MaterialService extends NamedIdCacheService {

    private final MaterialRepository materialRepository = new MaterialRepository();

    public MaterialService() {
        super("materials");
    }

    public boolean ensure(ItemStack stack) {
        return ensure(ServiceSupport.itemName(stack));
    }

    public boolean ensure(String name) {
        String normalized = normalize(name);
        if (normalized == null || !ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        if (!materialRepository.insert(normalized)) {
            return false;
        }
        rememberPending(normalized);
        return true;
    }

    @Override
    protected String normalize(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.startsWith("minecraft:") ? name.substring("minecraft:".length()) : name;
    }
}
