package top.saltwood.dologger.command.filter;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum Filters {
    ACTION("action"),
    USER("user"),
    INCLUDE("include"),
    EXCLUDE("exclude"),
    RADIUS("radius"),
    TIME("time"),
    LIMIT("limit");

    private final String prefix;

    Filters(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    static Optional<Filters> byPrefix(String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(filter -> filter.prefix.equals(normalized)).findFirst();
    }
}
