package top.saltwood.dologger.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.util.EntityUtils;

public class EntityEvents {

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        LivingEntity killed = event.getEntity();
        Level level = killed.level();
        if (level.isClientSide()) {
            return;
        }

        DamageSource source = event.getSource();
        EntityUtils.Actor actor = EntityUtils.resolveLogicalActor(source.getEntity());
        if (actor == null) {
            return;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return;
        }

        services.block().insertEntity(actor.uuid(), actor.name(), level, killed.blockPosition(), killed, BlockAction.KILL_ENTITY);
    }
}
