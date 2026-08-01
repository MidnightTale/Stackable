package net.hynse.stackable.config;

import org.bukkit.Material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A named stack-size tier. Grant {@link #permission()} to a LuckPerms group/rank
 * so those players get this tier's item limits.
 */
public final class StackGroup {
    private final String name;
    private final String permission;
    private final Map<Material, Integer> materials;
    private final Map<String, Integer> wildcards;

    public StackGroup(
            String name,
            String permission,
            Map<Material, Integer> materials,
            Map<String, Integer> wildcards
    ) {
        this.name = name;
        this.permission = permission;
        this.materials = Collections.unmodifiableMap(new LinkedHashMap<>(materials));
        this.wildcards = Collections.unmodifiableMap(new LinkedHashMap<>(wildcards));
    }

    public String name() {
        return name;
    }

    public String permission() {
        return permission;
    }

    public Map<Material, Integer> materials() {
        return materials;
    }

    public Map<String, Integer> wildcards() {
        return wildcards;
    }

    public boolean isEmpty() {
        return materials.isEmpty() && wildcards.isEmpty();
    }
}
