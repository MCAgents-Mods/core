package io.github.mcagentsmods.core.common.hologram;

/**
 * A point or direction in world space, in blocks.
 *
 * @param x east
 * @param y up
 * @param z south
 */
public record Vector3(double x, double y, double z) {

    /** The origin. */
    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Turns this vector about the vertical axis, the way Minecraft's {@code Axis.YP} rotation
     * does: a positive angle carries +x towards -z.
     *
     * @param degrees the angle
     * @return the turned vector
     */
    public Vector3 rotateY(double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector3(x * cos + z * sin, y, -x * sin + z * cos);
    }
}
