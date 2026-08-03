package net.hynse.stackable.listener;

import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Handles player interactions with stackable items
 */
public class PlayerListener implements Listener {
    private final StackSizeManager stackSizeManager;
    private final WrappedScheduler scheduler;

    public PlayerListener(StackSizeManager stackSizeManager, WrappedScheduler scheduler) {
        this.stackSizeManager = stackSizeManager;
        this.scheduler = scheduler;
    }

    // Fix for placing a stack of powder snow buckets causing the whole stack to be deleted
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent e) {
        Player player = e.getPlayer();
        ItemStack itemInHand = e.getItemInHand();
        if (itemInHand.getType() == Material.POWDER_SNOW_BUCKET && itemInHand.getAmount() > 1) {
            e.setCancelled(true);
            BlockState state = e.getBlockPlaced().getState();
            this.scheduler.runTaskAtLocation(e.getBlock().getLocation(), () -> {
                state.update(true);
                ItemStack emptyBucket = ItemStack.of(Material.BUCKET, 1);
                stackSizeManager.applyCustomStackSize(emptyBucket, player);
                itemInHand.subtract();
                player.give(emptyBucket);
            });
        }
    }

    // Handles stackable item with durability
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemDamage(final PlayerItemDamageEvent e) {
        final ItemStack item = e.getItem();
        if (item.getAmount() > 1 && e.getDamage() > 0) {
            // Split the stack
            final ItemStack split = item.clone();
            split.subtract();
            item.setAmount(1);
            // Give the split stack after the original item is damaged.
            scheduler.runTaskAtEntity(e.getPlayer(), () -> e.getPlayer().give(split));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLevelChange(CauldronLevelChangeEvent e) {
        if (e.getEntity() == null || !(e.getEntity() instanceof Player player)) {
            return;
        }
        if (e.getReason() == CauldronLevelChangeEvent.ChangeReason.BUCKET_EMPTY) {
            PlayerInventory inventory = player.getInventory();
            ItemStack itemUsed = inventory.getItemInMainHand();
            if (!itemUsed.getType().toString().contains("_BUCKET")) {
                itemUsed = inventory.getItemInOffHand();
            }

            // Cancel the event if the content of the cauldron and the bucket matches.
            String bucketTrimmed = itemUsed.getType().toString().replace("_BUCKET", "");
            String cauldronTrimmed = e.getBlock().getType().toString().replace("_CAULDRON", "");
            if (bucketTrimmed.equals(cauldronTrimmed)) {
                e.setCancelled(true);
            }
        }
    }
}
