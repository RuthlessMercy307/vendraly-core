package com.vendraly.core.rpg.ability;

/**
 * Define los tipos de habilidades secundarias que un jugador puede subir de nivel.
 * Usado por AbilityManager para gestionar experiencia, niveles y progresión.
 *
 * NOTA: Los nombres de los enums (TAILORING, BLACKSMITHING, etc.)
 * deben coincidir con las claves usadas en RPGStats si se referencian allí.
 */
public enum AbilityType {

    // ===============================
    //   HABILIDADES BASADAS EN EXP
    // ===============================

    /** Confección de armaduras de cuero, malla o tela. */
    TAILORING("Sastrería",
            "Mejora armaduras crafteadas (cuero, malla), añadiendo Defensa Mágica.",
            100, false),

    /** Herrería: armas y herramientas de metal. */
    BLACKSMITHING("Herrería",
            "Mejora armas y herramientas crafteadas (metal), añadiendo Daño o Velocidad.",
            100, false),

    /** Alquimia: creación de pociones y consumibles. */
    APOTHECARY("Boticario",
            "Mejora pociones crafteadas, aumentando duración y potencia.",
            100, false),

    /** Minería de recursos minerales. */
    MINING("Minería",
            "Aumenta probabilidad de drops dobles y mejora velocidad de minado.",
            100, false),

    /** Tala de árboles. */
    WOODCUTTING("Tala",
            "Aumenta probabilidad de troncos dobles y mejora velocidad de tala.",
            100, false),

    /** Agricultura. */
    FARMING("Agricultura",
            "Mejora rendimiento de cultivos y acelera crecimiento.",
            100, false),

    /** Crafteo genérico (cuando no aplica otra habilidad). */
    CRAFTING("Crafteo General",
            "Habilidad genérica usada para crafteos sin una habilidad especializada.",
            50, false),

    // ===============================
    //   HABILIDADES BASADAS EN PUNTOS
    // ===============================

    /** Agilidad / Robo: requiere invertir puntos. */
    AGILITY("Agilidad",
            "Mejora la capacidad de robo, manipulación de inventarios y trampas.",
            50, true);

    // ===============================
    //   CAMPOS Y CONSTRUCTOR
    // ===============================

    private final String displayName;
    private final String description;
    private final int maxLevel;
    private final boolean usesPoints;

    AbilityType(String displayName, String description, int maxLevel, boolean usesPoints) {
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
        this.usesPoints = usesPoints;
    }

    // ===============================
    //   GETTERS
    // ===============================

    /** Nombre legible para mostrar en menús o lore. */
    public String getDisplayName() {
        return displayName;
    }

    /** Breve descripción de la habilidad. */
    public String getDescription() {
        return description;
    }

    /** Nivel máximo de progresión. */
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Indica si la habilidad se gestiona con inversión de puntos
     * en lugar de progresar con experiencia.
     */
    public boolean usesPoints() {
        return usesPoints;
    }
}
