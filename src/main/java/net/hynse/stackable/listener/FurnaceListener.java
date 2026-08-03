package net.hynse.stackable.listener;

import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Handles interactions between furnace and stackable items
 */
public class FurnaceListener implements Listener {
    private final StackSizeManager stackSizeManager;

    public FurnaceListener(StackSizeManager stackSizeManager) {
        this.stackSizeManager = stackSizeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFuelUsed(FurnaceBurnEvent e) {
        ItemStack fuel = e.getFuel();
        if (fuel.getType() == Material.LAVA_BUCKET && fuel.getAmount() > 1) {
            // Eject an empty bucket
            ItemStack bucket = ItemStack.of(Material.BUCKET, 1);
            boolean bucketMoved = false;
            if (e.getBlock().getRelative(BlockFace.DOWN).getState() instanceof Hopper hopper) {
                Inventory hopperInventory = hopper.getInventory();
                bucketMoved = hopperInventory.addItem(bucket).isEmpty();
            }
            if (!bucketMoved) {
                e.getBlock().getWorld().dropItem(e.getBlock().getLocation(), bucket);
            }
        }
    }
}
