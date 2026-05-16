package top.saltwood.dologger.command.filter;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TimeFilter(long cutoffMillis) {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(\\d+)([mhdy])$", Pattern.CASE_INSENSITIVE);
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    private static final long YEAR = 365L * DAY;

    static TimeFilter parse(String value) throws FilterParseException {
        Matcher matcher = TIME_PATTERN.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; expected time.<number><m|h|d|y>");
        }

        long amount;
        try {
            amount = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; number is too large");
        }
        if (amount <= 0) {
            throw new FilterParseException("Invalid time filter value '" + value + "'; number must be positive");
        }

        long unit = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
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
        return new TimeFilter(System.currentTimeMillis() - elapsed);
    }

    Object queryValue() {
        return cutoffMillis;
    }
}
