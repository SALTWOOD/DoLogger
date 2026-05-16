package top.saltwood.dologger.command.filter;

import net.minecraft.core.BlockPos;

public record RadiusFilter(int radius, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {

    static RadiusFilter parse(String value, BlockPos origin) throws FilterParseException {
        if (origin == null) {
            throw new FilterParseException("radius filter requires a player position");
        }
        int radius;
        try {
            radius = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new FilterParseException("Invalid radius filter value '" + value + "'; expected radius.<integer>");
        }
        if (radius < 0) {
            throw new FilterParseException("Invalid radius filter value '" + value + "'; radius must be non-negative");
        }
        return new RadiusFilter(
                radius,
                origin.getX() - radius,
                origin.getX() + radius,
                origin.getY() - radius,
                origin.getY() + radius,
                origin.getZ() - radius,
                origin.getZ() + radius
        );
    }

    Object queryMinXValue() {
        return minX;
    }

    Object queryMaxXValue() {
        return maxX;
    }

    Object queryMinYValue() {
        return minY;
    }

    Object queryMaxYValue() {
        return maxY;
    }

    Object queryMinZValue() {
        return minZ;
    }

    Object queryMaxZValue() {
        return maxZ;
    }
}
