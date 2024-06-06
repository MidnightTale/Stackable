package net.hynse.stackable;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Stackable extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        setStackableItems();
    }

    private void setStackableItems() {
        // Stackable Potions
        setMaxStackSize(Material.POTION, 16);

        for (Material material : Material.values()) {
            if (material.name().endsWith("SHULKER_BOX")) {
                setMaxStackSize(material, 16);
            }
        }
        for (Material material : Material.values()) {
            if (material.name().endsWith("BED")) {
                setMaxStackSize(material, 8);
            }
        }


        setMaxStackSize(Material.MINECART, 16);
        setMaxStackSize(Material.CHEST_MINECART, 16);
        setMaxStackSize(Material.FURNACE_MINECART, 16);
        setMaxStackSize(Material.TNT_MINECART, 16);
        setMaxStackSize(Material.HOPPER_MINECART, 16);

        setMaxStackSize(Material.OAK_BOAT, 16);
        setMaxStackSize(Material.SPRUCE_BOAT, 16);
        setMaxStackSize(Material.BIRCH_BOAT, 16);
        setMaxStackSize(Material.JUNGLE_BOAT, 16);
        setMaxStackSize(Material.ACACIA_BOAT, 16);
        setMaxStackSize(Material.DARK_OAK_BOAT, 16);
        setMaxStackSize(Material.MANGROVE_BOAT, 16);
        setMaxStackSize(Material.CHERRY_BOAT,16);
        setMaxStackSize(Material.OAK_CHEST_BOAT, 16);
        setMaxStackSize(Material.SPRUCE_CHEST_BOAT, 16);
        setMaxStackSize(Material.BIRCH_CHEST_BOAT, 16);
        setMaxStackSize(Material.JUNGLE_CHEST_BOAT, 16);
        setMaxStackSize(Material.ACACIA_CHEST_BOAT, 16);
        setMaxStackSize(Material.DARK_OAK_CHEST_BOAT, 16);
        setMaxStackSize(Material.MANGROVE_CHEST_BOAT, 16);
        setMaxStackSize(Material.CHERRY_CHEST_BOAT, 16);

        // Stackable Totems
        setMaxStackSize(Material.TOTEM_OF_UNDYING, 4);
    }

    private void setMaxStackSize(Material material, int maxStackSize) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setMaxStackSize(maxStackSize);
            itemStack.setItemMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        setStackableItems();
    }

    @EventHandler
    public void onCraftEvent(CraftItemEvent event) {
        setStackableItems();
    }
}
