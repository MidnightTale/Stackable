package net.hynse.stackable.manager;

import net.hynse.stackable.Stackable;
import net.hynse.stackable.config.ConfigManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages stack-size metadata and safely splits stacks when a player's
 * effective permission changes.
 */
public class StackSizeManager {
    private final ConfigManager configManager;
    private final NamespacedKey managedStackKey;

    public StackSizeManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.managedStackKey = new NamespacedKey(Stackable.instance, "managed_stack_size");
    }

    public boolean canUse(Permissible permissible) {
        return configManager.canUse(permissible);
    }

    public int getPermissionState(Permissible permissible) {
        return configManager.getPermissionState(permissible);
    }

    /**
     * Whether non-player sources should apply base stack sizes.
     * Disabled in permission mode because world sources have no player context.
     */
    public boolean applyWithoutPlayer() {
        return !configManager.requiresPermission() && configManager.hasBaseItems();
    }

    public void applyCustomStackSize(ItemStack item) {
        applyCustomStackSize(item, null);
    }

    /**
     * Applies the effective custom size. If no custom size applies to the
     * player anymore, stale Stackable metadata is removed immediately.
     */
    public void applyCustomStackSize(ItemStack item, Permissible who) {
        if (item == null || item.getType().isAir()) return;

        int maxStackSize;
        if (who == null) {
            if (!applyWithoutPlayer()) return;
            if (!configManager.isConfiguredMaterial(item.getType())) return;
            maxStackSize = configManager.getMaxStackSize(item.getType());
        } else {
            maxStackSize = configManager.getEffectiveMaxStackSize(item.getType(), who);
            if (maxStackSize < 1) {
                restoreVanillaStackSize(item);
                return;
            }
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean changed = false;
        if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != maxStackSize) {
            meta.setMaxStackSize(maxStackSize);
            changed = true;
        }
        if (!meta.getPersistentDataContainer().has(managedStackKey, PersistentDataType.BYTE)) {
            meta.getPersistentDataContainer().set(managedStackKey, PersistentDataType.BYTE, (byte) 1);
            changed = true;
        }
        if (changed) {
            item.setItemMeta(meta);
        }
    }

    /**
     * Removes metadata created by Stackable. The configured-material fallback
     * also cleans items produced by versions before the PDC marker existed.
     */
    public boolean restoreVanillaStackSize(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        boolean marked = meta.getPersistentDataContainer()
                .has(managedStackKey, PersistentDataType.BYTE);
        boolean legacyManaged = configManager.isKnownConfiguredMaterial(item.getType())
                && meta.hasMaxStackSize();

        if (!marked && !legacyManaged) return false;

        if (meta.hasMaxStackSize()) {
            meta.setMaxStackSize(null);
        }
        meta.getPersistentDataContainer().remove(managedStackKey);
        item.setItemMeta(meta);
        return true;
    }

    public boolean isManaged(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(managedStackKey, PersistentDataType.BYTE)
                || (configManager.isKnownConfiguredMaterial(item.getType()) && meta.hasMaxStackSize());
    }

    /**
     * Maximum amount this material may have for the given player. This uses
     * the Material vanilla limit instead of possibly stale ItemMeta.
     */
    public int getAllowedMaxStackSize(ItemStack item, Permissible who) {
        if (item == null || item.getType().isAir()) return 0;

        if (who != null) {
            int custom = configManager.getEffectiveMaxStackSize(item.getType(), who);
            return custom > 0 ? custom : item.getType().getMaxStackSize();
        }

        if (applyWithoutPlayer() && configManager.isConfiguredMaterial(item.getType())) {
            return configManager.getMaxStackSize(item.getType());
        }
        return item.getType().getMaxStackSize();
    }

    public void applyCustomStackSizeToAll(ItemStack[] items) {
        applyCustomStackSizeToAll(items, null);
    }

    public void applyCustomStackSizeToAll(ItemStack[] items, Permissible who) {
        if (items == null) return;
        for (ItemStack item : items) {
            applyCustomStackSize(item, who);
        }
    }

    public void restoreVanillaStackSizeToAll(ItemStack[] items) {
        if (items == null) return;
        for (ItemStack item : items) {
            restoreVanillaStackSize(item);
        }
    }

    public void enforceInventory(Inventory inventory, Player player) {
        if (inventory == null) return;
        if (player != null && !canUse(player)) {
            restoreInventory(inventory, player);
            return;
        }

        List<ItemStack> overflow = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            applyCustomStackSize(item, player);
            int max = getAllowedMaxStackSize(item, player);
            inventory.setItem(slot, item);

            if (max < 1 || item.getAmount() <= max) continue;
            splitOversizedSlot(inventory, slot, item, max, player, overflow);
        }

        giveOverflow(overflow, inventory, player);
    }

    public void restoreInventory(Inventory inventory, Player player) {
        if (inventory == null) return;

        List<ItemStack> overflow = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            boolean managed = isManaged(item);
            if (!managed) continue;

            restoreVanillaStackSize(item);
            int max = item.getType().getMaxStackSize();
            inventory.setItem(slot, item);

            if (item.getAmount() <= max) continue;
            splitOversizedSlot(inventory, slot, item, max, null, overflow);
        }

        giveOverflow(overflow, inventory, player);
    }

    private void splitOversizedSlot(
            Inventory inventory,
            int slot,
            ItemStack item,
            int max,
            Player player,
            List<ItemStack> overflow
    ) {
        int excess = item.getAmount() - max;
        item.setAmount(max);
        inventory.setItem(slot, item);

        while (excess > 0) {
            int take = Math.min(max, excess);
            ItemStack split = item.clone();
            split.setAmount(take);
            if (player != null) {
                applyCustomStackSize(split, player);
            } else {
                restoreVanillaStackSize(split);
            }
            overflow.add(split);
            excess -= take;
        }
    }

    public void enforcePlayerInventories(Player player) {
        if (player == null || !player.isOnline()) return;

        if (!canUse(player)) {
            restorePlayerInventories(player);
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        enforceInventory(playerInventory, player);

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top != playerInventory) {
            enforceInventory(top, player);
        }

        enforceCursor(player, true);
    }

    public void restorePlayerInventories(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory playerInventory = player.getInventory();
        restoreInventory(playerInventory, player);

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top != playerInventory) {
            restoreInventory(top, player);
        }

        enforceCursor(player, false);
    }

    /** Refreshes permissions and sends the changed inventory to the client. */
    public void refreshPlayer(Player player) {
        enforcePlayerInventories(player);
        if (player != null && player.isOnline()) {
            player.updateInventory();
        }
    }

    private void enforceCursor(Player player, boolean applyCustom) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir()) return;

        boolean managedBefore = isManaged(cursor);
        if (applyCustom) {
            applyCustomStackSize(cursor, player);
        } else {
            if (!managedBefore) return;
            restoreVanillaStackSize(cursor);
        }

        int max = applyCustom
                ? getAllowedMaxStackSize(cursor, player)
                : cursor.getType().getMaxStackSize();

        if (max < 1 || cursor.getAmount() <= max) {
            player.setItemOnCursor(cursor);
            return;
        }

        int excess = cursor.getAmount() - max;
        cursor.setAmount(max);
        player.setItemOnCursor(cursor);

        PlayerInventory playerInventory = player.getInventory();
        while (excess > 0) {
            int take = Math.min(max, excess);
            ItemStack split = cursor.clone();
            split.setAmount(take);
            if (applyCustom) {
                applyCustomStackSize(split, player);
            } else {
                restoreVanillaStackSize(split);
            }
            Map<Integer, ItemStack> failed = playerInventory.addItem(split);
            dropFailed(player, failed);
            excess -= take;
        }
    }

    private void giveOverflow(List<ItemStack> overflow, Inventory inventory, Player player) {
        if (overflow.isEmpty()) return;

        Inventory destination = player != null ? player.getInventory() : inventory;
        for (ItemStack extra : overflow) {
            HashMap<Integer, ItemStack> failed = destination.addItem(extra);
            if (failed.isEmpty()) continue;

            if (player != null) {
                dropFailed(player, failed);
                continue;
            }

            Location location = inventory.getLocation();
            if (location != null && location.getWorld() != null) {
                for (ItemStack leftover : failed.values()) {
                    location.getWorld().dropItemNaturally(location, leftover);
                }
            } else {
                Stackable.instance.getLogger().warning(
                        "Could not place " + failed.values().size()
                                + " overflow stack(s) from an inventory without a location."
                );
            }
        }
    }

    private void dropFailed(Player player, Map<Integer, ItemStack> failed) {
        for (ItemStack leftover : failed.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
