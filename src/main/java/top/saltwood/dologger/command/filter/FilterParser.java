package top.saltwood.dologger.command.filter;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class FilterParser {

    private FilterParser() {
    }

    public static FilterList parse(@Nullable String input, @Nullable BlockPos origin) throws FilterParseException {
        Builder builder = new Builder();
        if (input == null || input.isBlank()) {
            return builder.build();
        }

        String[] tokens = input.trim().split("\\s+");
        for (String token : tokens) {
            int separator = token.indexOf('.');
            if (separator <= 0 || separator == token.length() - 1) {
                throw new FilterParseException("Invalid filter '" + token + "'; expected <prefix>.<value>");
            }

            String prefix = token.substring(0, separator);
            String value = token.substring(separator + 1);
            Filters filter = Filters.byPrefix(prefix).orElseThrow(() -> new FilterParseException("Unknown filter prefix '" + prefix + "'"));
            builder.set(filter, value, origin);
        }
        return builder.build();
    }

    private static final class Builder {
        private UserFilter user;
        private TimeFilter time;
        private RadiusFilter radius;
        private ActionFilter action;
        private IncludeFilter include;
        private ExcludeFilter exclude;
        private LimitFilter limit;

        void set(Filters filter, String value, @Nullable BlockPos origin) throws FilterParseException {
            switch (filter) {
                case ACTION -> {
                    rejectDuplicate(action, filter);
                    action = ActionFilter.parse(value);
                }
                case USER -> {
                    rejectDuplicate(user, filter);
                    user = UserFilter.parse(value);
                }
                case TIME -> {
                    rejectDuplicate(time, filter);
                    time = TimeFilter.parse(value);
                }
                case RADIUS -> {
                    rejectDuplicate(radius, filter);
                    radius = RadiusFilter.parse(value, origin);
                }
                case INCLUDE -> {
                    rejectDuplicate(include, filter);
                    include = IncludeFilter.parse(value);
                }
                case EXCLUDE -> {
                    rejectDuplicate(exclude, filter);
                    exclude = ExcludeFilter.parse(value);
                }
                case LIMIT -> {
                    rejectDuplicate(limit, filter);
                    limit = LimitFilter.parse(value);
                }
            }
        }

        FilterList build() {
            return new FilterList(user, time, radius, action, include, exclude, limit);
        }

        private static void rejectDuplicate(Object existing, Filters filter) throws FilterParseException {
            if (existing != null) {
                throw new FilterParseException("Duplicate " + filter.prefix() + " filter");
            }
        }
    }
}
