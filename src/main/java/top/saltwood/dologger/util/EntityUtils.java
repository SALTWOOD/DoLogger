package top.saltwood.dologger.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.action.BlockAction;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EntityUtils {

    private EntityUtils() {
    }

    public record Actor(UUID uuid, String name) {
    }

    public static Actor resolveLogicalActor(@Nullable Entity entity) {
        if (entity == null) {
            return null;
        }
        return resolveLogicalActor(entity, new HashSet<>());
    }

    private static Actor resolveLogicalActor(Entity entity, Set<UUID> seen) {
        if (!seen.add(entity.getUUID())) {
            return new Actor(getCombinedUUID(entity), getCombinedName(entity));
        }

        if (entity instanceof ServerPlayer player) {
            return new Actor(player.getUUID(), player.getGameProfile().getName());
        }

        if (entity instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null) {
                return resolveLogicalActor(owner, seen);
            }
        }

        if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner != null) {
                return resolveLogicalActor(owner, seen);
            }
        }

        if (entity instanceof Mob mob) {
            Entity target = mob.getTarget();
            if (target != null) {
                return resolveLogicalActor(target, seen);
            }
        }

        return new Actor(getCombinedUUID(entity), getCombinedName(entity));
    }

    public static UUID getCombinedUUID(Entity entity) {
        return UUID.nameUUIDFromBytes(("DoLogger:" + entityName(entity)).getBytes(StandardCharsets.UTF_8));
    }

    public static String getCombinedName(Entity entity) {
        return "Entity:" + entityName(entity).getPath();
    }

    public static boolean logBlockAction(@Nullable Entity source, Level level, BlockPos pos, Block block, BlockAction action) {
        if (level.isClientSide()) {
            return false;
        }

        Services services = Dologger.getServices();
        if (services == null) {
            return false;
        }

        Actor actor = resolveLogicalActor(source);
        if (actor == null) {
            return false;
        }

        String material = BuiltInRegistries.BLOCK.getKey(block).toString();
        return services.block().insertBlock(actor.uuid(), actor.name(), level, pos, material, action);
    }

    private static net.minecraft.resources.ResourceLocation entityName(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }
}
