package net.hynse.stackable.listener;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all events related to stackable items
 */
public class StackableListener implements Listener {
    private final StackSizeManager stackSizeManager;
    private final WrappedScheduler scheduler;

    public StackableListener(StackSizeManager stackSizeManager, WrappedScheduler scheduler) {
        this.stackSizeManager = stackSizeManager;
        this.scheduler = scheduler;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        if (!stackSizeManager.canUse(player)) {
            stackSizeManager.restoreVanillaStackSize(e.getCurrentItem());
            stackSizeManager.restoreVanillaStackSize(e.getCursor());
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.restorePlayerInventories(player));
            return;
        }

        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);
        if (e.getClick().isShiftClick() || e.getClick().isKeyboardClick()) {
            stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents(), player);
        }

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!stackSizeManager.canUse(player)) {
            e.getNewItems().values().forEach(stackSizeManager::restoreVanillaStackSize);
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.restorePlayerInventories(player));
            return;
        }
        e.getNewItems().values().forEach(item -> stackSizeManager.applyCustomStackSize(item, player));
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent e) {
        Player player = e.getPlayer();
        if (!stackSizeManager.canUse(player)) {
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.restorePlayerInventories(player));
            return;
        }
        stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents(), player);
        stackSizeManager.applyCustomStackSize(player.getInventory().getItemInOffHand(), player);
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        Player player = e.getView().getPlayer() instanceof Player p ? p : null;
        if (player != null) {
            if (!stackSizeManager.canUse(player)) return;
            CraftingInventory inventory = e.getInventory();
            stackSizeManager.applyCustomStackSize(inventory.getResult(), player);
            stackSizeManager.applyCustomStackSizeToAll(inventory.getMatrix(), player);
            return;
        }
        if (!stackSizeManager.applyWithoutPlayer()) return;
        CraftingInventory inventory = e.getInventory();
        stackSizeManager.applyCustomStackSize(inventory.getResult());
        stackSizeManager.applyCustomStackSizeToAll(inventory.getMatrix());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!stackSizeManager.canUse(player)) {
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.restorePlayerInventories(player));
            return;
        }

        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);
        if (e.getInventory().getResult() != null) {
            stackSizeManager.applyCustomStackSize(e.getInventory().getResult(), player);
        }

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!stackSizeManager.canUse(player)) {
            stackSizeManager.restoreInventory(player.getInventory(), player);
            return;
        }
        stackSizeManager.enforceInventory(e.getInventory(), player);
        stackSizeManager.enforceInventory(player.getInventory(), player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            if (!stackSizeManager.canUse(player)) {
                scheduler.runTaskAtEntity(player, () ->
                        stackSizeManager.restoreVanillaStackSize(e.getItem().getItemStack())
                );
                return;
            }
            scheduler.runTaskAtEntity(player, () ->
                    stackSizeManager.applyCustomStackSize(e.getItem().getItemStack(), player)
            );
            return;
        }
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtEntity(e.getEntity(), () ->
                stackSizeManager.applyCustomStackSize(e.getItem().getItemStack())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        Location location = e.getDestination().getLocation();
        if (location != null) {
            scheduler.runTaskAtLocation(location, () ->
                    stackSizeManager.applyCustomStackSize(e.getItem())
            );
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
                stackSizeManager.applyCustomStackSize(e.getItem())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtEntity(e.getEntity(), () ->
                stackSizeManager.applyCustomStackSize(e.getEntity().getItemStack())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCrafter(CrafterCraftEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
                stackSizeManager.applyCustomStackSize(e.getResult())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
                stackSizeManager.applyCustomStackSize(e.getResult())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        scheduler.runTaskAtLocation(e.getItem().getLocation(), () ->
                stackSizeManager.applyCustomStackSize(e.getItem().getItemStack())
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!stackSizeManager.canUse(player)) {
            stackSizeManager.restoreVanillaStackSize(e.getCurrentItem());
            stackSizeManager.restoreVanillaStackSize(e.getCursor());
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.restorePlayerInventories(player));
            return;
        }
        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    public void fixInventory(Inventory inv) {
        if (inv == null) return;
        stackSizeManager.applyCustomStackSizeToAll(inv.getContents());
    }
}
