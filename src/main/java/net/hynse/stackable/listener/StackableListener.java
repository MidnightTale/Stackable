package net.hynse.stackable.listener;

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
import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
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

        // Apply immediately so subsequent vanilla stack logic sees the custom max
        stackSizeManager.applyCustomStackSize(e.getCurrentItem());
        stackSizeManager.applyCustomStackSize(e.getCursor());
        if (e.getClick().isShiftClick() || e.getClick().isKeyboardClick()) {
            stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents());
        }

        // Recipe-book / shift moves finish after this event; re-enforce next tick
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        e.getNewItems().values().forEach(stackSizeManager::applyCustomStackSize);
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    /**
     * Recipe book clicks rebuild ingredient stacks from material counts and ignore
     * per-stack max_stack_size components. Ensure inventory items are tagged first,
     * then split any oversized stacks the placer created.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent e) {
        Player player = e.getPlayer();
        stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents());
        stackSizeManager.applyCustomStackSize(player.getInventory().getItemInOffHand());
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inventory = e.getInventory();
        stackSizeManager.applyCustomStackSize(inventory.getResult());
        stackSizeManager.applyCustomStackSizeToAll(inventory.getMatrix());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        stackSizeManager.applyCustomStackSize(e.getCurrentItem());
        stackSizeManager.applyCustomStackSize(e.getCursor());
        if (e.getInventory().getResult() != null) {
            stackSizeManager.applyCustomStackSize(e.getInventory().getResult());
        }

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        stackSizeManager.enforceInventory(e.getInventory(), player);
        stackSizeManager.enforceInventory(player.getInventory(), player);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityPickupItemEvent e) {
        scheduler.runTaskAtEntity(e.getEntity(), () ->
            stackSizeManager.applyCustomStackSize(e.getItem().getItemStack())
        );
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        Location location = e.getDestination().getLocation();
        if (location != null) {
            scheduler.runTaskAtLocation(location, () ->
                stackSizeManager.applyCustomStackSize(e.getItem())
            );
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
            stackSizeManager.applyCustomStackSize(e.getItem())
        );
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent e) {
        scheduler.runTaskAtEntity(e.getEntity(), () -> {
            ItemStack item = e.getEntity().getItemStack();
            stackSizeManager.applyCustomStackSize(item);
        });
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPrepareCrafter(CrafterCraftEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
            stackSizeManager.applyCustomStackSize(e.getResult())
        );
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () ->
            stackSizeManager.applyCustomStackSize(e.getResult())
        );
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        scheduler.runTaskAtLocation(e.getItem().getLocation(), () ->
            stackSizeManager.applyCustomStackSize(e.getItem().getItemStack())
        );
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        stackSizeManager.applyCustomStackSize(e.getCurrentItem());
        stackSizeManager.applyCustomStackSize(e.getCursor());
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }
    
    /**
     * Applies custom stack sizes to all items in an inventory
     * @param inv The inventory to process
     */
    public void fixInventory(Inventory inv) {
        if (inv == null) return;
        stackSizeManager.applyCustomStackSizeToAll(inv.getContents());
    }
}
