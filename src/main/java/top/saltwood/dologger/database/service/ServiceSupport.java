package top.saltwood.dologger.database.service;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.database.DatabaseManager;
import top.saltwood.dologger.database.SqlQueue;
import top.saltwood.dologger.model.SimpleItemStack;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

final class ServiceSupport {

    private ServiceSupport() {
    }

    static boolean canWrite() {
        return DatabaseStatus.isAvailable() && Dologger.getSqlQueue() != null;
    }

    static boolean canRead() {
        return DatabaseStatus.isAvailable();
    }

    static void logUnavailable() {
        DatabaseManager databaseManager = Dologger.getDatabaseManager();
        if (databaseManager != null) {
            databaseManager.logBoundedError(DatabaseStatus.unavailableMessage());
        }
    }

    static boolean enqueue(SqlQueue.SqlTask task) {
        if (!canWrite()) {
            logUnavailable();
            return false;
        }
        return Dologger.getSqlQueue().enqueue(task);
    }

    static <T> List<T> readList(Supplier<List<T>> supplier) {
        if (!canRead()) {
            logUnavailable();
            return Collections.emptyList();
        }
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            if (exception.getCause() instanceof SQLException) {
                logSqlFailure((SQLException) exception.getCause());
                return Collections.emptyList();
            }
            throw exception;
        }
    }

    static void logSqlFailure(SQLException exception) {
        DatabaseManager databaseManager = Dologger.getDatabaseManager();
        if (databaseManager != null) {
            databaseManager.logBoundedError("Database read failed: " + exception.getMessage());
        }
    }

    static String levelName(Level level) {
        return level.dimension().location().toString();
    }

    static BlockPos blockPosition(ServerPlayer player) {
        return player.blockPosition();
    }

    static String itemName(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.toString();
    }

    static String entityName(Entity entity) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key.toString();
    }

    static SimpleItemStack simpleItemStack(ItemStack stack) {
        return SimpleItemStack.of(stack);
    }
}
