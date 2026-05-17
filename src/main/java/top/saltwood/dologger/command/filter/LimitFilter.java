package top.saltwood.dologger.command.filter;

public record LimitFilter(int value) {

    static LimitFilter parse(String value) throws FilterParseException {
        int parsed;
        try {
            parsed = Integer.parseInt(value == null ? "" : value.trim());
        } catch (NumberFormatException exception) {
            throw new FilterParseException("Invalid limit filter value '" + value + "'; expected limit.<positive number>");
        }
        if (parsed <= 0) {
            throw new FilterParseException("Invalid limit filter value '" + value + "'; number must be positive");
        }
        return new LimitFilter(parsed);
    }

    Object queryValue() {
        return value;
    }
}
