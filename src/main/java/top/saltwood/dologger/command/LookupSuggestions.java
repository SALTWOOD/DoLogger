package top.saltwood.dologger.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class LookupSuggestions {

    static final SuggestionProvider<CommandSourceStack> FILTERS = LookupSuggestions::suggest;

    private static final List<String> PREFIXES = List.of("action.", "user.", "include.", "exclude.", "radius.", "time.", "limit.");
    private static final List<String> ACTIONS = List.of(
            "break_block", "place_block", "interact_block", "kill_entity", "interact_entity",
            "remove_item", "add_item", "drop_item", "pickup_item", "craft_item", "break_item",
            "consume_item", "throw_item", "shoot_item", "add_item_ender", "remove_item_ender",
            "join", "quit", "chat", "command");
    private static final List<String> RADII = List.of("radius.b", "radius.5", "radius.10", "radius.25", "radius.1c", "radius.2c");
    private static final List<String> TIMES = List.of("time.30s", "time.30m", "time.1h", "time.1d", "time.7d", "time.>30m", "time.>1h", "time.>1d");
    private static final List<String> LIMITS = List.of("limit.10", "limit.50", "limit.100", "limit.500", "limit.1000");

    private LookupSuggestions() {
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = StringArgumentType.getString(context, "filters");
        Token token = Token.from(input);
        SuggestionsBuilder scopedBuilder = builder.createOffset(builder.getStart() + token.start());
        String partial = token.partial().toLowerCase(Locale.ROOT);

        suggestionsFor(context, input, token.partial())
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(partial))
                .forEach(scopedBuilder::suggest);
        return scopedBuilder.buildFuture();
    }

    private static Stream<String> suggestionsFor(CommandContext<CommandSourceStack> context, String input, String partial) {
        int dot = partial.indexOf('.');
        if (dot < 0) {
            Set<String> used = usedPrefixes(input, partial);
            return PREFIXES.stream().filter(prefix -> !used.contains(prefix.substring(0, prefix.length() - 1)));
        }

        String prefix = partial.substring(0, dot).toLowerCase(Locale.ROOT);
        String values = partial.substring(dot + 1);
        String valuePrefix = prefix + "." + completedValues(values);
        return switch (prefix) {
            case "action" -> ACTIONS.stream().map(valuePrefix::concat);
            case "user" -> context.getSource().getServer().getPlayerList().getPlayers().stream().map(player -> valuePrefix + player.getGameProfile().getName());
            case "include", "exclude" -> materialNames().map(valuePrefix::concat);
            case "radius" -> RADII.stream();
            case "time" -> TIMES.stream();
            case "limit" -> LIMITS.stream();
            default -> Stream.empty();
        };
    }

    private static Set<String> usedPrefixes(String input, String partial) {
        Set<String> used = new HashSet<>();
        String committedInput = input.substring(0, Math.max(0, input.length() - partial.length())).trim();
        if (committedInput.isEmpty()) {
            return used;
        }
        for (String token : committedInput.split("\\s+")) {
            int dot = token.indexOf('.');
            if (dot > 0) {
                used.add(token.substring(0, dot).toLowerCase(Locale.ROOT));
            }
        }
        return used;
    }

    private static String completedValues(String values) {
        int comma = values.lastIndexOf(',');
        return comma < 0 ? "" : values.substring(0, comma + 1);
    }

    private static Stream<String> materialNames() {
        return Stream.concat(BuiltInRegistries.BLOCK.keySet().stream(), BuiltInRegistries.ITEM.keySet().stream())
                .map(LookupSuggestions::normalizeMaterial)
                .distinct();
    }

    private static String normalizeMaterial(ResourceLocation id) {
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private record Token(int start, String partial) {

        static Token from(String input) {
            int start = Math.max(input.lastIndexOf(' '), input.lastIndexOf('\t')) + 1;
            return new Token(start, input.substring(start));
        }
    }
}
