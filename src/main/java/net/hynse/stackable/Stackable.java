package net.hynse.stackable;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Stackable extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

//    @EventHandler
//    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
//        ItemStack result = event.getInventory().getResult();
//        if (result != null && isStackableItem(result.getType())) {
//            ItemMeta meta = result.getItemMeta();
//            if (meta != null) {
//                meta.setMaxStackSize(getMaxStackSize(result.getType()));
//                result.setItemMeta(meta);
//            }
//        }
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && isStackableItem(result.getType())) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.setMaxStackSize(getMaxStackSize(result.getType()));
                result.setItemMeta(meta);
            }
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (isStackableItem(item.getType())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setMaxStackSize(getMaxStackSize(item.getType()));
                item.setItemMeta(meta);
            }
        }
    }


    private boolean isStackableItem(Material material) {
        return switch (material) {
            case POTION, LINGERING_POTION, SPLASH_POTION, MINECART, CHEST_MINECART, FURNACE_MINECART, TNT_MINECART, HOPPER_MINECART, OAK_BOAT,
                 SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT, DARK_OAK_BOAT, MANGROVE_BOAT, CHERRY_BOAT,
                 OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT, ACACIA_CHEST_BOAT,
                 DARK_OAK_CHEST_BOAT, MANGROVE_CHEST_BOAT, CHERRY_CHEST_BOAT, TOTEM_OF_UNDYING, SHULKER_BOX, WHITE_SHULKER_BOX,
                 ORANGE_SHULKER_BOX, MAGENTA_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX,
                 PINK_SHULKER_BOX, GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX,
                 BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX, BLACK_SHULKER_BOX, WHITE_BED,
                 ORANGE_BED, MAGENTA_BED, LIGHT_BLUE_BED, YELLOW_BED, LIME_BED, PINK_BED, GRAY_BED, LIGHT_GRAY_BED,
                 CYAN_BED, PURPLE_BED, BLUE_BED, BROWN_BED, GREEN_BED, RED_BED, BLACK_BED -> true;
            default -> false;
        };
    }

    private int getMaxStackSize(Material material) {
        return switch (material) {
            case POTION, LINGERING_POTION, SPLASH_POTION, MINECART, CHEST_MINECART, FURNACE_MINECART, TNT_MINECART, HOPPER_MINECART, OAK_BOAT,
                 SPRUCE_BOAT, BIRCH_BOAT, JUNGLE_BOAT, ACACIA_BOAT, DARK_OAK_BOAT, MANGROVE_BOAT, CHERRY_BOAT,
                 OAK_CHEST_BOAT, SPRUCE_CHEST_BOAT, BIRCH_CHEST_BOAT, JUNGLE_CHEST_BOAT, ACACIA_CHEST_BOAT,
                 DARK_OAK_CHEST_BOAT, MANGROVE_CHEST_BOAT, CHERRY_CHEST_BOAT, SHULKER_BOX, WHITE_SHULKER_BOX, ORANGE_SHULKER_BOX,
                 MAGENTA_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, YELLOW_SHULKER_BOX, LIME_SHULKER_BOX, PINK_SHULKER_BOX,
                 GRAY_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, CYAN_SHULKER_BOX, PURPLE_SHULKER_BOX, BLUE_SHULKER_BOX,
                 BROWN_SHULKER_BOX, GREEN_SHULKER_BOX, RED_SHULKER_BOX, BLACK_SHULKER_BOX -> 16;
            case TOTEM_OF_UNDYING -> 4;
            case WHITE_BED, ORANGE_BED, MAGENTA_BED, LIGHT_BLUE_BED, YELLOW_BED, LIME_BED, PINK_BED, GRAY_BED,
                 LIGHT_GRAY_BED, CYAN_BED, PURPLE_BED, BLUE_BED, BROWN_BED, GREEN_BED, RED_BED, BLACK_BED -> 8;
            default -> material.getMaxStackSize();
        };
    }
}
