package net.hynse.stackable.manager;

import net.hynse.stackable.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permissible;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages stack size operations for items
 */
public class StackSizeManager {
    private final ConfigManager configManager;

    public StackSizeManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public boolean canUse(Permissible permissible) {
        return configManager.canUse(permissible);
    }

    /**
     * Whether non-player sources should apply base stack sizes.
     * Off in permission mode so only ranked players get the perk.
     */
    public boolean applyWithoutPlayer() {
        return !configManager.requiresPermission() && configManager.hasBaseItems();
    }

    /** Base/world apply (no player context). */
    public void applyCustomStackSize(ItemStack item) {
        applyCustomStackSize(item, null);
    }

    /**
     * Applies the effective custom stack size for this player (base + groups).
     * {@code who == null} uses base items only (and only when {@link #applyWithoutPlayer()}).
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
            if (maxStackSize < 1) return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != maxStackSize) {
                meta.setMaxStackSize(maxStackSize);
                item.setItemMeta(meta);
            }
        }
    }

    public void restoreVanillaStackSize(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        if (!configManager.isConfiguredMaterial(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasMaxStackSize()) {
            meta.setMaxStackSize(null);
            item.setItemMeta(meta);
        }
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

            int max = resolveMax(item, player);
            if (max < 1 || item.getAmount() <= max) continue;

            int excess = item.getAmount() - max;
            item.setAmount(max);
            inventory.setItem(slot, item);

            while (excess > 0) {
                int take = Math.min(max, excess);
                ItemStack split = item.clone();
                split.setAmount(take);
                applyCustomStackSize(split, player);
                overflow.add(split);
                excess -= take;
            }
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

            restoreVanillaStackSize(item);
            if (!configManager.isConfiguredMaterial(item.getType())) continue;

            int max = item.getMaxStackSize();
            if (item.getAmount() <= max) continue;

            int excess = item.getAmount() - max;
            item.setAmount(max);
            inventory.setItem(slot, item);

            while (excess > 0) {
                int take = Math.min(max, excess);
                ItemStack split = item.clone();
                split.setAmount(take);
                restoreVanillaStackSize(split);
                overflow.add(split);
                excess -= take;
            }
        }

        giveOverflow(overflow, inventory, player);
    }

    public void enforcePlayerInventories(Player player) {
        if (player == null) return;

        if (!canUse(player)) {
            restorePlayerInventories(player);
            return;
        }

        PlayerInventory playerInventory = player.getInventory();
        applyCustomStackSizeToAll(playerInventory.getContents(), player);
        applyCustomStackSize(playerInventory.getItemInOffHand(), player);
        enforceInventory(playerInventory, player);

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top != playerInventory) {
            enforceInventory(top, player);
        }

        enforceCursor(player, true);
    }

    public void restorePlayerInventories(Player player) {
        if (player == null) return;

        PlayerInventory playerInventory = player.getInventory();
        restoreVanillaStackSizeToAll(playerInventory.getContents());
        restoreVanillaStackSize(playerInventory.getItemInOffHand());
        restoreInventory(playerInventory, player);
        enforceCursor(player, false);
    }

    private void enforceCursor(Player player, boolean applyCustom) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType().isAir()) return;
        if (!configManager.isConfiguredMaterial(cursor.getType())) return;

        if (applyCustom) {
            applyCustomStackSize(cursor, player);
        } else {
            restoreVanillaStackSize(cursor);
        }

        int max = applyCustom ? resolveMax(cursor, player) : cursor.getMaxStackSize();
        if (max < 1 || cursor.getAmount() <= max) {
            player.setItemOnCursor(cursor);
            return;
        }

        int excess = cursor.getAmount() - max;
        cursor.setAmount(max);
        if (applyCustom) {
            applyCustomStackSize(cursor, player);
        } else {
            restoreVanillaStackSize(cursor);
        }
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
            for (ItemStack leftover : failed.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            excess -= take;
        }
    }

    private int resolveMax(ItemStack item, Player player) {
        if (player != null) {
            return configManager.getEffectiveMaxStackSize(item.getType(), player);
        }
        if (!configManager.isConfiguredMaterial(item.getType())) {
            return -1;
        }
        return configManager.getMaxStackSize(item.getType());
    }

    private void giveOverflow(List<ItemStack> overflow, Inventory inventory, Player player) {
        if (overflow.isEmpty()) return;

        Inventory destination = player != null ? player.getInventory() : inventory;
        for (ItemStack extra : overflow) {
            HashMap<Integer, ItemStack> failed = destination.addItem(extra);
            if (player != null) {
                for (ItemStack leftover : failed.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            } else if (!failed.isEmpty()) {
                for (ItemStack leftover : failed.values()) {
                    inventory.addItem(leftover);
                }
            }
        }
    }
}
