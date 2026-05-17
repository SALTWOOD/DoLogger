package top.saltwood.dologger.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import top.saltwood.dologger.Dologger;
import top.saltwood.dologger.command.filter.FilterList;
import top.saltwood.dologger.command.filter.FilterParseException;
import top.saltwood.dologger.command.filter.FilterParser;
import top.saltwood.dologger.database.service.DatabaseStatus;
import top.saltwood.dologger.database.service.Services;
import top.saltwood.dologger.model.BlockPosition;
import top.saltwood.dologger.model.action.BlockAction;
import top.saltwood.dologger.model.history.BlockHistory;
import top.saltwood.dologger.permission.Permissions;
import top.saltwood.dologger.util.LanguageResolver;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RevertCommand {

    private static final long PLAN_TTL_MILLIS = 60_000L;
    private static final Set<String> SUPPORTED_ACTIONS = Set.of("break_block", "place_block");
    private static final Map<UUID, PendingPlan> PENDING_PLANS = new ConcurrentHashMap<>();

    private RevertCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("revert")
                .then(Commands.literal("preview")
                        .then(Commands.argument("filters", StringArgumentType.greedyString())
                                .suggests(LookupSuggestions.FILTERS)
                                .executes(context -> preview(context.getSource(), StringArgumentType.getString(context, "filters")))))
                .then(Commands.literal("confirm")
                        .executes(context -> confirm(context.getSource())))
                .then(Commands.literal("cancel")
                        .executes(context -> cancel(context.getSource())));
    }

    private static int preview(CommandSourceStack source, String filters) {
        Services services = validate(source);
        if (services == null) {
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        if (filters == null || filters.isBlank()) {
            source.sendFailure(LanguageResolver.component("dologger.commands.revert.empty_filters"));
            return 0;
        }
        if (!validateRawActionFilter(source, filters)) {
            return 0;
        }

        FilterList filterList;
        try {
            filterList = FilterParser.parse(filters, player.blockPosition());
        } catch (FilterParseException exception) {
            source.sendFailure(LanguageResolver.component("dologger.commands.lookup.parse_error", exception.getMessage()));
            return 0;
        }

        List<BlockHistory> history = services.block().getRevertCandidates(player.level(), filterList.toBlockRepositoryParams());
        if (history.isEmpty()) {
            source.sendFailure(LanguageResolver.component("dologger.commands.revert.no_results"));
            return 0;
        }

        List<Integer> plannedIds = history.stream().map(BlockHistory::getId).toList();
        PENDING_PLANS.put(player.getUUID(), new PendingPlan(plannedIds, player.level().dimension(), System.currentTimeMillis() + PLAN_TTL_MILLIS));
        source.sendSuccess(() -> LanguageResolver.component("dologger.commands.revert.preview", plannedIds.size()), false);
        source.sendSuccess(() -> LanguageResolver.component("dologger.commands.revert.confirm_hint"), false);
        return plannedIds.size();
    }

    private static int confirm(CommandSourceStack source) {
        Services services = validate(source);
        if (services == null) {
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }

        PendingPlan plan = PENDING_PLANS.get(player.getUUID());
        if (plan == null || plan.isExpired()) {
            PENDING_PLANS.remove(player.getUUID());
            source.sendFailure(LanguageResolver.component("dologger.commands.revert.no_pending"));
            return 0;
        }
        PENDING_PLANS.remove(player.getUUID());

        Level level = player.server.getLevel(plan.dimension());
        if (level == null) {
            source.sendFailure(LanguageResolver.component("dologger.commands.revert.dimension_unavailable"));
            return 0;
        }

        RevertCounts counts = executePlan(services, player, level, plan.entryIds());
        source.sendSuccess(() -> LanguageResolver.component("dologger.commands.revert.confirmed", counts.success(), counts.skipped(), counts.conflict()), true);
        return counts.success();
    }

    private static int cancel(CommandSourceStack source) {
        if (!Permissions.canRevert(source)) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.permission"));
            return 0;
        }
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.players_only"));
            return 0;
        }
        PENDING_PLANS.remove(player.getUUID());
        source.sendSuccess(() -> LanguageResolver.component("dologger.commands.revert.cancelled"), false);
        return 1;
    }

    private static Services validate(CommandSourceStack source) {
        if (!Permissions.canRevert(source)) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.permission"));
            return null;
        }
        Services services = Dologger.getServices();
        if (services == null || !services.isDatabaseAvailable()) {
            source.sendFailure(DatabaseStatus.unavailableComponent());
            return null;
        }
        if (source.getPlayer() == null) {
            source.sendFailure(LanguageResolver.component("dologger.commands.error.players_only"));
            return null;
        }
        return services;
    }

    private static boolean validateRawActionFilter(CommandSourceStack source, String filters) {
        Set<String> requested = new HashSet<>();
        for (String token : filters.trim().split("\\s+")) {
            int separator = token.indexOf('.');
            if (separator <= 0) {
                continue;
            }
            String prefix = token.substring(0, separator).toLowerCase(Locale.ROOT);
            if (!"action".equals(prefix)) {
                continue;
            }
            for (String action : token.substring(separator + 1).split(",")) {
                String normalized = action.toLowerCase(Locale.ROOT).trim();
                if (!SUPPORTED_ACTIONS.contains(normalized)) {
                    source.sendFailure(LanguageResolver.component("dologger.commands.revert.unsupported_action", action));
                    return false;
                }
                requested.add(normalized);
            }
        }
        if (requested.isEmpty()) {
            source.sendFailure(LanguageResolver.component("dologger.commands.revert.action_required"));
            return false;
        }
        return true;
    }

    private static RevertCounts executePlan(Services services, ServerPlayer player, Level level, List<Integer> entryIds) {
        int success = 0;
        int skipped = 0;
        int conflict = 0;
        Set<Integer> pending = new HashSet<>(entryIds);
        UUID batch = UUID.randomUUID();
        List<BlockHistory> entries = services.block().getRevertCandidates(level, null).stream()
                .filter(entry -> pending.contains(entry.getId()))
                .toList();
        for (BlockHistory entry : entries) {
            RevertResult result = revertEntry(services, player, level, entry, batch);
            switch (result) {
                case SUCCESS -> success++;
                case SKIPPED -> skipped++;
                case CONFLICT -> conflict++;
            }
        }
        conflict += pending.size() - entries.size();
        return new RevertCounts(success, skipped, conflict);
    }

    private static RevertResult revertEntry(Services services, ServerPlayer player, Level level, BlockHistory entry, UUID batch) {
        BlockPos pos = toBlockPos(entry.getPosition());
        if (entry.getAction() == BlockAction.PLACE_BLOCK) {
            Block loggedBlock = resolveBlock(entry.getMaterial());
            if (loggedBlock == null) {
                return RevertResult.SKIPPED;
            }
            Block currentBlock = level.getBlockState(pos).getBlock();
            if (currentBlock != loggedBlock) {
                return RevertResult.CONFLICT;
            }
            BlockState beforeState = level.getBlockState(pos);
            BlockState targetState = Blocks.AIR.defaultBlockState();
            if (!level.setBlockAndUpdate(pos, targetState)) {
                return RevertResult.CONFLICT;
            }
            return finishRevert(services, player, level, pos, beforeState, targetState, loggedBlock, BlockAction.BREAK_BLOCK, entry.getId(), batch);
        }
        if (entry.getAction() == BlockAction.BREAK_BLOCK) {
            if (!level.getBlockState(pos).isAir()) {
                return RevertResult.CONFLICT;
            }
            Block loggedBlock = resolveBlock(entry.getMaterial());
            if (loggedBlock == null || loggedBlock == Blocks.AIR || loggedBlock instanceof net.minecraft.world.level.block.EntityBlock) {
                return RevertResult.SKIPPED;
            }
            BlockState beforeState = level.getBlockState(pos);
            BlockState targetState = loggedBlock.defaultBlockState();
            if (!level.setBlockAndUpdate(pos, targetState)) {
                return RevertResult.CONFLICT;
            }
            return finishRevert(services, player, level, pos, beforeState, targetState, loggedBlock, BlockAction.PLACE_BLOCK, entry.getId(), batch);
        }
        return RevertResult.SKIPPED;
    }

    private static RevertResult finishRevert(Services services, ServerPlayer player, Level level, BlockPos pos, BlockState beforeState, BlockState targetState, Block loggedBlock, BlockAction generatedAction, int sourceBlockId, UUID batch) {
        if (services.block().markRevertedWithGeneratedBlock(sourceBlockId, player.getUUID(), player.getGameProfile().getName(), level, pos, loggedBlock, generatedAction, System.currentTimeMillis(), batch)) {
            return RevertResult.SUCCESS;
        }
        rollbackWorld(level, pos, beforeState, targetState);
        return RevertResult.CONFLICT;
    }

    private static void rollbackWorld(Level level, BlockPos pos, BlockState beforeState, BlockState expectedState) {
        if (level.getBlockState(pos).equals(expectedState)) {
            level.setBlockAndUpdate(pos, beforeState);
        }
    }

    private static BlockPos toBlockPos(BlockPosition position) {
        return new BlockPos(position.x(), position.y(), position.z());
    }

    private static Block resolveBlock(String material) {
        ResourceLocation id = ResourceLocation.tryParse(material);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(id);
    }

    private enum RevertResult {
        SUCCESS,
        SKIPPED,
        CONFLICT
    }

    private record PendingPlan(List<Integer> entryIds, ResourceKey<Level> dimension, long expiresAtMillis) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }

    private record RevertCounts(int success, int skipped, int conflict) {
    }
}
