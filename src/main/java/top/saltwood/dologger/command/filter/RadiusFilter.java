package top.saltwood.dologger.command.filter;

import net.minecraft.core.BlockPos;

public record RadiusFilter(int radius, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {

    static RadiusFilter parse(String value, BlockPos origin) throws FilterParseException {
        if (origin == null) {
            throw new FilterParseException("radius filter requires a player position");
        }

        // Suffix-based units
        if (value.endsWith("b")) {
            return parseBlockRadius(value, origin);
        }
        if (value.endsWith("c")) {
            return parseChunkRadius(value, origin);
        }

        // Default: plain integer = block radius (legacy behavior)
        int radius;
        try {
            radius = Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new FilterParseException(
                    "Invalid radius filter value '" + value
                    + "'; expected examples: radius.10, radius.b, radius.1c, radius.2c");
        }
        if (radius < 0) {
            throw new FilterParseException("Invalid radius filter value '" + value + "'; radius must be non-negative");
        }
        return blockRadius(radius, origin);
    }

    private static RadiusFilter blockRadius(int radius, BlockPos origin) {
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

    private static RadiusFilter parseBlockRadius(String value, BlockPos origin) throws FilterParseException {
        String numPart = value.substring(0, value.length() - 1);
        int radius;
        if (numPart.isEmpty()) {
            // bare "b" = exact current block
            radius = 0;
        } else {
            try {
                radius = Integer.parseInt(numPart);
            } catch (NumberFormatException exception) {
                throw new FilterParseException(
                        "Invalid radius filter value '" + value
                        + "'; expected examples: radius.b, radius.5, radius.1c, radius.2c");
            }
            if (radius < 0) {
                throw new FilterParseException(
                        "Invalid radius filter value '" + value + "'; radius must be non-negative");
            }
        }
        return blockRadius(radius, origin);
    }

    private static RadiusFilter parseChunkRadius(String value, BlockPos origin) throws FilterParseException {
        String numPart = value.substring(0, value.length() - 1);
        int chunkRadius;
        if (numPart.isEmpty()) {
            chunkRadius = 1;
        } else {
            try {
                chunkRadius = Integer.parseInt(numPart);
            } catch (NumberFormatException exception) {
                throw new FilterParseException(
                        "Invalid radius filter value '" + value
                        + "'; expected examples: radius.1c, radius.2c, radius.b, radius.10");
            }
            if (chunkRadius < 1) {
                throw new FilterParseException(
                        "Invalid radius filter value '" + value + "'; chunk radius must be at least 1");
            }
        }

        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;
        int spread = chunkRadius - 1;

        int minChunkX = originChunkX - spread;
        int maxChunkX = originChunkX + spread;
        int minChunkZ = originChunkZ - spread;
        int maxChunkZ = originChunkZ + spread;

        // Convert chunk bounds to block bounds (inclusive)
        int minX = minChunkX << 4;
        int maxX = (maxChunkX << 4) + 15;
        int minZ = minChunkZ << 4;
        int maxZ = (maxChunkZ << 4) + 15;

        // Chunk lookup covers all Y values
        int minY = Integer.MIN_VALUE;
        int maxY = Integer.MAX_VALUE;

        return new RadiusFilter(
                chunkRadius,
                minX, maxX,
                minY, maxY,
                minZ, maxZ
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
