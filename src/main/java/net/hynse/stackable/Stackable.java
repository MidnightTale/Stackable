package net.hynse.stackable;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Stackable extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void setMaxStackSize(ItemStack itemStack, int maxStackSize) {
        if (itemStack != null) {
            Material material = itemStack.getType();
            if (isStackableItem(material)) {
                itemStack.setAmount(Math.min(itemStack.getAmount(), maxStackSize));
            }
        }
    }

    private boolean isStackableItem(Material material) {
        switch (material) {
            case POTION:
            case MINECART:
            case CHEST_MINECART:
            case FURNACE_MINECART:
            case TNT_MINECART:
            case HOPPER_MINECART:
            case OAK_BOAT:
            case SPRUCE_BOAT:
            case BIRCH_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
            case MANGROVE_BOAT:
            case CHERRY_BOAT:
            case OAK_CHEST_BOAT:
            case SPRUCE_CHEST_BOAT:
            case BIRCH_CHEST_BOAT:
            case JUNGLE_CHEST_BOAT:
            case ACACIA_CHEST_BOAT:
            case DARK_OAK_CHEST_BOAT:
            case MANGROVE_CHEST_BOAT:
            case CHERRY_CHEST_BOAT:
            case TOTEM_OF_UNDYING:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
            case WHITE_BED:
            case ORANGE_BED:
            case MAGENTA_BED:
            case LIGHT_BLUE_BED:
            case YELLOW_BED:
            case LIME_BED:
            case PINK_BED:
            case GRAY_BED:
            case LIGHT_GRAY_BED:
            case CYAN_BED:
            case PURPLE_BED:
            case BLUE_BED:
            case BROWN_BED:
            case GREEN_BED:
            case RED_BED:
            case BLACK_BED:
                return true;
            default:
                return false;
        }
    }

    private int getMaxStackSize(Material material) {
        switch (material) {
            case POTION:
            case LINGERING_POTION:
            case SPLASH_POTION:
            case MINECART:
            case CHEST_MINECART:
            case FURNACE_MINECART:
            case TNT_MINECART:
            case HOPPER_MINECART:
            case OAK_BOAT:
            case SPRUCE_BOAT:
            case BIRCH_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
            case MANGROVE_BOAT:
            case CHERRY_BOAT:
            case OAK_CHEST_BOAT:
            case SPRUCE_CHEST_BOAT:
            case BIRCH_CHEST_BOAT:
            case JUNGLE_CHEST_BOAT:
            case ACACIA_CHEST_BOAT:
            case DARK_OAK_CHEST_BOAT:
            case MANGROVE_CHEST_BOAT:
            case CHERRY_CHEST_BOAT:
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
                return 16;
            case TOTEM_OF_UNDYING:
                return 4;
            case WHITE_BED:
            case ORANGE_BED:
            case MAGENTA_BED:
            case LIGHT_BLUE_BED:
            case YELLOW_BED:
            case LIME_BED:
            case PINK_BED:
            case GRAY_BED:
            case LIGHT_GRAY_BED:
            case CYAN_BED:
            case PURPLE_BED:
            case BLUE_BED:
            case BROWN_BED:
            case GREEN_BED:
            case RED_BED:
            case BLACK_BED:
                return 8;
            default:
                return material.getMaxStackSize();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateInventory(event.getPlayer().getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        if (currentItem != null && isStackableItem(currentItem.getType())) {
            setMaxStackSize(currentItem, getMaxStackSize(currentItem.getType()));
        }
    }

    private void updateInventory(Inventory inventory) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack != null && isStackableItem(itemStack.getType())) {
                setMaxStackSize(itemStack, getMaxStackSize(itemStack.getType()));
            }
        }
    }
}
