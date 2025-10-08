package com.vendraly.core.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Roles predeterminados del servidor.
 */
public enum Role {
    OWNER("§4[Owner] ", NamedTextColor.DARK_RED, Arrays.asList("vendraly.*")),
    ADMIN("§c[Admin] ", NamedTextColor.RED, Arrays.asList("vendraly.admin", "vendraly.moderate")),
    MODERATOR("§9[Mod] ", NamedTextColor.BLUE, Collections.singletonList("vendraly.moderate")),
    VIP("§6[VIP] ", NamedTextColor.GOLD, Collections.singletonList("vendraly.vip")),
    CIVILIAN("", NamedTextColor.WHITE, Collections.emptyList());

    private final String prefix;
    private final NamedTextColor color;
    private final List<String> permissions;

    Role(String prefix, NamedTextColor color, List<String> permissions) {
        this.prefix = prefix;
        this.color = color;
        this.permissions = permissions;
    }

    public String getPrefix() {
        return prefix;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public Component getDisplayName(String name) {
        return Component.text(prefix + name, color);
    }
}
