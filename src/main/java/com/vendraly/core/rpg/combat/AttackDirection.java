package com.vendraly.core.rpg.combat;

/**
 * Define las 5 direcciones fijas de ataque disponibles para el jugador.
 */
public enum AttackDirection {
    // Horizontal y Vertical
    UP,
    LEFT,
    RIGHT,
    // Diagonales (Abajo se asume como movimiento vertical)
    DIAGONAL_LEFT_DOWN,
    DIAGONAL_RIGHT_DOWN,
    // Por defecto, o si no hay movimiento significativo
    NEUTRAL
}