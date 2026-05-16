package top.saltwood.dologger.database.service;

import net.minecraft.world.level.Level;
import top.saltwood.dologger.database.repository.LevelRepository;

public class LevelService extends NamedIdCacheService {

    private final LevelRepository levelRepository = new LevelRepository();

    public LevelService() {
        super("levels");
    }

    public boolean ensure(Level level) {
        return ensure(ServiceSupport.levelName(level));
    }

    public boolean ensure(String name) {
        if (normalize(name) == null || !ServiceSupport.canWrite()) {
            ServiceSupport.logUnavailable();
            return false;
        }
        levelRepository.insert(name);
        rememberPending(name);
        return true;
    }
}
