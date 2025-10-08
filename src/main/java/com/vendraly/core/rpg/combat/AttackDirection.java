package com.vendraly.core.rpg.combat;

/**
 * Direcciones de ataque para el combate direccional.
 */
public enum AttackDirection {
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST;

    public static AttackDirection fromYaw(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) {
            yaw += 360;
        }
        int index = Math.round(yaw / 45f) % values().length;
        return values()[index];
    }
}
