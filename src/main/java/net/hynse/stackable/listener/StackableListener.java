package net.hynse.stackable.listener;

import org.bukkit.event.player.PlayerRecipeBookClickEvent;
import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles all events related to stackable items. */
public class StackableListener implements Listener {
    private static final long PERMISSION_CHECK_TICKS = 20L;

    private final StackSizeManager stackSizeManager;
    private final WrappedScheduler scheduler;
    private final Map<UUID, Integer> permissionStates = new ConcurrentHashMap<>();

    public StackableListener(StackSizeManager stackSizeManager, WrappedScheduler scheduler) {
        this.stackSizeManager = stackSizeManager;
        this.scheduler = scheduler;
    }

    /**
     * Bukkit does not expose a universal permission-changed event. This cheap
     * state check works with LuckPerms and other permission plugins while only
     * scanning inventories when the player's effective Stackable access changes.
     */
    public void startPermissionRefreshMonitor() {
        scheduler.runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                scheduler.runTaskAtEntity(player, () -> refreshIfPermissionChanged(player));
            }
        }, PERMISSION_CHECK_TICKS, PERMISSION_CHECK_TICKS);
    }

    public void rememberPermissionState(Player player) {
        if (player != null) {
            permissionStates.put(player.getUniqueId(), stackSizeManager.getPermissionState(player));
        }
    }

    private void refreshIfPermissionChanged(Player player) {
        if (player == null || !player.isOnline()) return;

        int current = stackSizeManager.getPermissionState(player);
        Integer previous = permissionStates.put(player.getUniqueId(), current);
        if (previous != null && previous != current) {
            stackSizeManager.refreshPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        scheduler.runTaskAtEntity(player, () -> {
            stackSizeManager.refreshPlayer(player);
            rememberPermissionState(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent e) {
        permissionStates.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);

        if (e.getClick().isShiftClick() || e.getClick().isKeyboardClick()) {
            stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents(), player);
        }

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        e.getNewItems().values()
                .forEach(item -> stackSizeManager.applyCustomStackSize(item, player));
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent e) {
        Player player = e.getPlayer();
        stackSizeManager.applyCustomStackSizeToAll(player.getInventory().getContents(), player);
        stackSizeManager.applyCustomStackSize(player.getInventory().getItemInOffHand(), player);
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inventory = e.getInventory();
        Player player = e.getView().getPlayer() instanceof Player p ? p : null;

        if (player != null) {
            ItemStack result = inventory.getResult();
            stackSizeManager.applyCustomStackSize(result, player);
            inventory.setResult(result);

            ItemStack[] matrix = inventory.getMatrix();
            stackSizeManager.applyCustomStackSizeToAll(matrix, player);
            inventory.setMatrix(matrix);
            return;
        }

        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack result = inventory.getResult();
        stackSizeManager.applyCustomStackSize(result);
        inventory.setResult(result);

        ItemStack[] matrix = inventory.getMatrix();
        stackSizeManager.applyCustomStackSizeToAll(matrix);
        inventory.setMatrix(matrix);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);

        ItemStack result = e.getInventory().getResult();
        stackSizeManager.applyCustomStackSize(result, player);
        e.getInventory().setResult(result);

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        stackSizeManager.enforceInventory(e.getInventory(), player);
        stackSizeManager.enforceInventory(player.getInventory(), player);
    }

    /**
     * Normalize before vanilla inserts the entity into an inventory. The old
     * code waited a tick, when the item entity could already be removed.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            normalizeGroundItem(e.getItem(), player);
            scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
            return;
        }

        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack stack = e.getItem().getItemStack();
        stackSizeManager.applyCustomStackSize(stack);
        e.getItem().setItemStack(stack);
    }

    /**
     * A denied player may still be holding metadata from before permissions
     * changed. Strip it before the drop can merge with another ground item.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        Player player = e.getPlayer();
        Item dropped = e.getItemDrop();
        ItemStack stack = dropped.getItemStack();
        stackSizeManager.applyCustomStackSize(stack, player);
        dropped.setItemStack(stack);

        int max = stackSizeManager.getAllowedMaxStackSize(stack, player);
        if (max > 0 && stack.getAmount() > max) {
            // Defer splitting until cancellation is final. A cancelled drop
            // invalidates the item entity, so this task will not duplicate it.
            scheduler.runTaskLaterAtEntity(dropped, () -> splitGroundItem(dropped, max), 1L);
        }

        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;

        ItemStack source = e.getEntity().getItemStack();
        ItemStack target = e.getTarget().getItemStack();
        stackSizeManager.applyCustomStackSize(source);
        stackSizeManager.applyCustomStackSize(target);
        e.getEntity().setItemStack(source);
        e.getTarget().setItemStack(target);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;

        Location location = e.getDestination().getLocation();
        if (location == null) return;
        scheduler.runTaskLaterAtLocation(
                location,
                () -> stackSizeManager.enforceInventory(e.getDestination(), null),
                1L
        );
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack item = e.getItem();
        stackSizeManager.applyCustomStackSize(item);
        e.setItem(item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack item = e.getEntity().getItemStack();
        stackSizeManager.applyCustomStackSize(item);
        e.getEntity().setItemStack(item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareCrafter(CrafterCraftEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack result = e.getResult();
        stackSizeManager.applyCustomStackSize(result);
        e.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack result = e.getResult();
        stackSizeManager.applyCustomStackSize(result);
        e.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        if (!stackSizeManager.applyWithoutPlayer()) return;
        ItemStack item = e.getItem().getItemStack();
        stackSizeManager.applyCustomStackSize(item);
        e.getItem().setItemStack(item);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryCreative(InventoryCreativeEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        stackSizeManager.applyCustomStackSize(e.getCurrentItem(), player);
        stackSizeManager.applyCustomStackSize(e.getCursor(), player);
        scheduler.runTaskAtEntity(player, () -> stackSizeManager.enforcePlayerInventories(player));
    }

    /**
     * Ensures the entity itself never carries more than the receiver may hold.
     * Overflow is re-spawned in correctly-sized entities on the next location tick.
     */
    private void normalizeGroundItem(Item entity, Player player) {
        ItemStack stack = entity.getItemStack();
        stackSizeManager.applyCustomStackSize(stack, player);
        entity.setItemStack(stack);

        int max = stackSizeManager.getAllowedMaxStackSize(stack, player);
        if (max > 0 && stack.getAmount() > max) {
            splitGroundItem(entity, max);
        }
    }

    private void splitGroundItem(Item entity, int max) {
        if (!entity.isValid()) return;

        ItemStack stack = entity.getItemStack();
        int originalAmount = stack.getAmount();
        if (max < 1 || originalAmount <= max) return;

        int firstAmount = Math.min(originalAmount, max);
        stack.setAmount(firstAmount);
        entity.setItemStack(stack);

        int remaining = originalAmount - firstAmount;
        Location location = entity.getLocation().clone();
        if (location.getWorld() == null) return;

        Vector velocity = entity.getVelocity().clone();
        int pickupDelay = entity.getPickupDelay();
        ItemStack template = stack.clone();

        while (remaining > 0) {
            int amount = Math.min(max, remaining);
            ItemStack split = template.clone();
            split.setAmount(amount);

            Item spawned = location.getWorld().dropItem(location, split);
            spawned.setVelocity(velocity);
            spawned.setPickupDelay(Math.max(1, pickupDelay));
            remaining -= amount;
        }
    }

    public void fixInventory(Inventory inventory) {
        if (inventory == null || !stackSizeManager.applyWithoutPlayer()) return;
        stackSizeManager.applyCustomStackSizeToAll(inventory.getContents());
    }
}
