package top.saltwood.dologger.database.service;

import net.minecraft.world.entity.Entity;
import top.saltwood.dologger.database.repository.EntityRepository;

public class EntityService extends NamedIdCacheService {

    private final EntityRepository entityRepository = new EntityRepository();

    public EntityService() {
        super("entities");
    }

    public boolean ensure(Entity entity) {
        return ensure(ServiceSupport.entityName(entity));
    }

    public boolean ensure(String name) {
        if (normalize(name) == null || !ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        if (!entityRepository.insert(name)) {
            return false;
        }
        rememberPending(name);
        return true;
    }
}
