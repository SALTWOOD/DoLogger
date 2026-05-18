package top.saltwood.dologger.command.filter;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TimeFilter(@Nullable Long afterCutoffMillis, @Nullable Long beforeCutoffMillis) {

    private static final Pattern TIME_PATTERN = Pattern.compile("^([<>]?)(\\d+)([smhdy])$", Pattern.CASE_INSENSITIVE);
    private static final long SECOND = 1000L;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    private static final long YEAR = 365L * DAY;

    static TimeFilter parse(String value) throws FilterParseException {
        Matcher matcher = TIME_PATTERN.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; expected time.<number><s|m|h|d|y>");
        }

        String direction = matcher.group(1);
        long amount;
        try {
            amount = Long.parseLong(matcher.group(2));
        } catch (NumberFormatException exception) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; number is too large");
        }
        if (amount <= 0) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; number must be positive");
        }

        long unit = switch (matcher.group(3).toLowerCase(Locale.ROOT)) {
            case "s" -> SECOND;
            case "m" -> MINUTE;
            case "h" -> HOUR;
            case "d" -> DAY;
            case "y" -> YEAR;
            default -> throw new FilterParseException("Invalid time filter unit in '" + value + "'");
        };

        long elapsed;
        try {
            elapsed = Math.multiplyExact(amount, unit);
        } catch (ArithmeticException exception) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; duration is too large");
        }

        long cutoff = System.currentTimeMillis() - elapsed;
        if (">".equals(direction)) {
            return new TimeFilter(null, cutoff);
        } else {
            return new TimeFilter(cutoff, null);
        }
    }
}
