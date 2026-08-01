package net.hynse.stackable.config;

import net.hynse.stackable.Stackable;
import net.hynse.stackable.util.RegexUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ConfigManager {
    public static final String PERMISSION_USE = "stackable.use";
    public static final String PERMISSION_GROUP_PREFIX = "stackable.group.";

    private final Map<Material, Integer> stackableItems = new LinkedHashMap<>();
    private final Map<String, Integer> wildcardPatterns = new LinkedHashMap<>();
    private final List<StackGroup> groups = new ArrayList<>();
    private final List<String> createdGroupPermissions = new ArrayList<>();
    private boolean requirePermission;

    public void loadConfig() {
        unregisterGroupPermissions();
        stackableItems.clear();
        wildcardPatterns.clear();
        groups.clear();
        RegexUtil.clearCache();

        FileConfiguration config = Stackable.instance.getConfig();
        requirePermission = config.getBoolean("require-permission", false);

        loadItemSection(config.getConfigurationSection("items"), stackableItems, wildcardPatterns, "items");
        expandWildcards(stackableItems, wildcardPatterns, "items");
        loadGroups(config.getConfigurationSection("groups"));
        registerGroupPermissions();

        Stackable.instance.getLogger().info(
                "Loaded " + stackableItems.size() + " base materials, "
                        + wildcardPatterns.size() + " base wildcards, and "
                        + groups.size() + " groups"
                        + (requirePermission ? " (permission mode on)" : "") + "."
        );
    }

    public void reloadConfig() {
        Stackable.instance.reloadConfig();
        loadConfig();
        Stackable.instance.getLogger().info(
                "Stackable configuration reloaded: "
                        + stackableItems.size() + " base items, "
                        + groups.size() + " groups."
        );
    }

    public boolean requiresPermission() {
        return requirePermission;
    }

    public List<StackGroup> getGroups() {
        return groups;
    }

    /**
     * True if this material appears in base items or any group (used when stripping meta).
     */
    public boolean isConfiguredMaterial(Material material) {
        if (lookupIn(stackableItems, wildcardPatterns, material) >= 0) {
            return true;
        }
        for (StackGroup group : groups) {
            if (lookupIn(group.materials(), group.wildcards(), material) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @deprecated prefer {@link #isConfiguredMaterial(Material)} or player-aware checks
     */
    @Deprecated
    public boolean isStackableItem(Material material) {
        return isConfiguredMaterial(material);
    }

    /**
     * Base-only stack size (no player). Used for hoppers/world when permission mode is off.
     */
    public int getMaxStackSize(Material material) {
        int size = lookupIn(stackableItems, wildcardPatterns, material);
        return size >= 0 ? size : material.getMaxStackSize();
    }

    /**
     * Effective max stack for this player: highest value among base (if allowed) and all groups they have.
     * Returns {@code -1} if nothing custom applies.
     */
    public int getEffectiveMaxStackSize(Material material, Permissible who) {
        int best = -1;

        if (hasBaseAccess(who)) {
            best = Math.max(best, lookupIn(stackableItems, wildcardPatterns, material));
        }

        if (who != null) {
            for (StackGroup group : groups) {
                if (!who.hasPermission(group.permission())) {
                    continue;
                }
                best = Math.max(best, lookupIn(group.materials(), group.wildcards(), material));
            }
        }

        return best;
    }

    /**
     * Whether this player should receive any custom stacks at all.
     */
    public boolean canUse(Permissible who) {
        if (who == null) {
            return !requirePermission && hasBaseItems();
        }
        if (hasAnyGroupPermission(who)) {
            return true;
        }
        if (!requirePermission) {
            return hasBaseItems();
        }
        return who.hasPermission(PERMISSION_USE) && hasBaseItems();
    }

    public boolean hasBaseAccess(Permissible who) {
        if (!hasBaseItems()) {
            return false;
        }
        if (!requirePermission) {
            return true;
        }
        return who != null && who.hasPermission(PERMISSION_USE);
    }

    public boolean hasAnyGroupPermission(Permissible who) {
        if (who == null) {
            return false;
        }
        for (StackGroup group : groups) {
            if (who.hasPermission(group.permission())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasBaseItems() {
        return !stackableItems.isEmpty() || !wildcardPatterns.isEmpty();
    }

    public int getDirectMaterialCount() {
        return stackableItems.size();
    }

    public int getWildcardPatternCount() {
        return wildcardPatterns.size();
    }

    public int getGroupCount() {
        return groups.size();
    }

    private void loadGroups(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            ConfigurationSection groupSection = section.getConfigurationSection(key);
            if (groupSection == null) {
                Stackable.instance.getLogger().warning("Invalid group section: " + key);
                continue;
            }

            String permission = groupSection.getString("permission", PERMISSION_GROUP_PREFIX + key.toLowerCase());
            Map<Material, Integer> materials = new LinkedHashMap<>();
            Map<String, Integer> wildcards = new LinkedHashMap<>();
            loadItemSection(groupSection.getConfigurationSection("items"), materials, wildcards, "groups." + key);
            expandWildcards(materials, wildcards, "groups." + key);

            StackGroup group = new StackGroup(key, permission, materials, wildcards);
            if (group.isEmpty()) {
                Stackable.instance.getLogger().warning("Group '" + key + "' has no items; skipped.");
                continue;
            }
            groups.add(group);

            if (Stackable.instance.getConfig().getBoolean("debug", false)) {
                Stackable.instance.getLogger().info("[DEBUG] Group '" + key + "' permission=" + permission
                        + " materials=" + materials.size() + " wildcards=" + wildcards.size());
            }
        }
    }

    private void loadItemSection(
            ConfigurationSection section,
            Map<Material, Integer> materials,
            Map<String, Integer> wildcards,
            String pathLabel
    ) {
        if (section == null) {
            return;
        }

        Set<String> keys = Objects.requireNonNull(section).getKeys(false);
        for (String key : keys) {
            int maxStackSize = section.getInt(key);
            if (maxStackSize < 1 || maxStackSize > 99) {
                Stackable.instance.getLogger().warning(
                        "Stack size for " + pathLabel + "." + key + " must be 1-99 (got " + maxStackSize + ")"
                );
                continue;
            }

            if (isWildcardPattern(key)) {
                wildcards.put(key, maxStackSize);
            } else {
                try {
                    materials.put(Material.valueOf(key.toUpperCase()), maxStackSize);
                } catch (IllegalArgumentException e) {
                    Stackable.instance.getLogger().warning("Invalid material in " + pathLabel + ": " + key);
                }
            }
        }
    }

    /**
     * Resolves wildcard patterns into concrete Material entries so undyed items
     * (e.g. SHULKER_BOX under *_shulker_box) and all music discs are registered
     * the same way as explicit materials.
     */
    private void expandWildcards(
            Map<Material, Integer> materials,
            Map<String, Integer> wildcards,
            String pathLabel
    ) {
        if (wildcards.isEmpty()) {
            return;
        }

        int before = materials.size();
        for (Material material : Material.values()) {
            if (material.isLegacy() || !material.isItem()) {
                continue;
            }
            int matched = RegexUtil.getBestMatchOrDefault(material, wildcards, -1);
            if (matched < 0) {
                continue;
            }
            Integer existing = materials.get(material);
            materials.put(material, existing == null ? matched : Math.max(existing, matched));
        }

        int added = materials.size() - before;
        Stackable.instance.getLogger().info(
                "Expanded " + wildcards.size() + " wildcard(s) in " + pathLabel
                        + " to " + added + " new material(s) (" + materials.size() + " total)."
        );
    }

    private static int lookupIn(
            Map<Material, Integer> materials,
            Map<String, Integer> wildcards,
            Material material
    ) {
        Integer direct = materials.get(material);
        int best = direct != null ? direct : -1;
        best = Math.max(best, RegexUtil.getBestMatchOrDefault(material, wildcards, -1));
        return best;
    }

    private boolean isWildcardPattern(String str) {
        if (str.contains("*")) {
            return true;
        }
        try {
            Material.valueOf(str.toUpperCase());
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private void registerGroupPermissions() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        for (StackGroup group : groups) {
            String node = group.permission();
            if (pluginManager.getPermission(node) != null) {
                continue;
            }
            Permission permission = new Permission(
                    node,
                    "Stackable stack group '" + group.name() + "'",
                    PermissionDefault.FALSE
            );
            pluginManager.addPermission(permission);
            createdGroupPermissions.add(node);
        }
    }

    private void unregisterGroupPermissions() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        for (String node : createdGroupPermissions) {
            Permission permission = pluginManager.getPermission(node);
            if (permission != null) {
                pluginManager.removePermission(permission);
            }
        }
        createdGroupPermissions.clear();
    }
}
