package net.hynse.stackable.manager;

import net.hynse.stackable.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

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
    
    /**
     * Applies custom stack size to an item if configured
     * @param item The item to modify
     */
    public void applyCustomStackSize(ItemStack item) {
        if (item == null || item.getType().isAir()) return;
        
        Material material = item.getType();
        if (!configManager.isStackableItem(material)) return;
        
        int maxStackSize = configManager.getMaxStackSize(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!meta.hasMaxStackSize() || meta.getMaxStackSize() != maxStackSize) {
                meta.setMaxStackSize(maxStackSize);
                item.setItemMeta(meta);
            }
        }
    }
    
    /**
     * Applies custom stack sizes to all items in an inventory
     * @param items Array of items to process
     */
    public void applyCustomStackSizeToAll(ItemStack[] items) {
        if (items == null) return;
        
        for (ItemStack item : items) {
            applyCustomStackSize(item);
        }
    }

    /**
     * Applies custom max stack sizes and splits any stacks that exceed the configured limit.
     * Excess items are returned to the player inventory, or dropped if there is no space.
     *
     * @param inventory inventory to enforce (crafting grid, chest, player inv, etc.)
     * @param player    player to receive overflow, or null to only shrink in place
     */
    public void enforceInventory(Inventory inventory, Player player) {
        if (inventory == null) return;

        List<ItemStack> overflow = new ArrayList<>();
        ItemStack[] contents = inventory.getContents();

        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()) continue;

            applyCustomStackSize(item);

            if (!configManager.isStackableItem(item.getType())) continue;

            int max = configManager.getMaxStackSize(item.getType());
            if (item.getAmount() <= max) continue;

            int excess = item.getAmount() - max;
            item.setAmount(max);
            inventory.setItem(slot, item);

            while (excess > 0) {
                int take = Math.min(max, excess);
                ItemStack split = item.clone();
                split.setAmount(take);
                applyCustomStackSize(split);
                overflow.add(split);
                excess -= take;
            }
        }

        if (overflow.isEmpty()) return;

        Inventory destination = player != null ? player.getInventory() : inventory;
        for (ItemStack extra : overflow) {
            HashMap<Integer, ItemStack> failed = destination.addItem(extra);
            if (player != null) {
                for (ItemStack leftover : failed.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            } else if (!failed.isEmpty()) {
                // No player and inventory is full: leave residual amount on first matching slot if possible
                for (ItemStack leftover : failed.values()) {
                    shrinkIntoExisting(inventory, leftover);
                }
            }
        }
    }

    /**
     * Enforces custom stack sizes across a player's open view (top + bottom inventories).
     * Used after recipe-book fills and shift-crafts, which rebuild stacks without max-stack meta.
     */
    public void enforcePlayerInventories(Player player) {
        if (player == null) return;

        PlayerInventory playerInventory = player.getInventory();
        applyCustomStackSizeToAll(playerInventory.getContents());
        applyCustomStackSize(playerInventory.getItemInOffHand());
        enforceInventory(playerInventory, player);

        Inventory top = player.getOpenInventory().getTopInventory();
        if (top != null && top != playerInventory) {
            enforceInventory(top, player);
        }

        applyCustomStackSize(player.getItemOnCursor());
        ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && !cursor.getType().isAir() && configManager.isStackableItem(cursor.getType())) {
            int max = configManager.getMaxStackSize(cursor.getType());
            if (cursor.getAmount() > max) {
                int excess = cursor.getAmount() - max;
                cursor.setAmount(max);
                applyCustomStackSize(cursor);
                player.setItemOnCursor(cursor);

                while (excess > 0) {
                    int take = Math.min(max, excess);
                    ItemStack split = cursor.clone();
                    split.setAmount(take);
                    applyCustomStackSize(split);
                    Map<Integer, ItemStack> failed = playerInventory.addItem(split);
                    for (ItemStack leftover : failed.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                    excess -= take;
                }
            }
        }
    }

    private void shrinkIntoExisting(Inventory inventory, ItemStack leftover) {
        applyCustomStackSize(leftover);
        // Best-effort: if we cannot place it, drop is impossible without a location; keep as no-op.
        inventory.addItem(leftover);
    }
}
