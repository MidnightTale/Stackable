package net.hynse.stackable;

import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.WrappedSchedulerBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Stackable extends JavaPlugin implements Listener {
    final WrappedSchedulerBuilder schedulerBuilder = WrappedSchedulerBuilder.builder().plugin(this);
    final WrappedScheduler scheduler = schedulerBuilder.build(); // Scheduler ready to use, yay!

    private final Map<Material, Integer> stackableItems = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadStackableItems();
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("stackablereload")).setExecutor(this);
    }
    public void reloadConfiguration() {
        reloadConfig();  // reloads config.yml from disk into memory
        stackableItems.clear();
        FileConfiguration config = getConfig();
        for (String key : Objects.requireNonNull(config.getConfigurationSection("stackable-items")).getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                int max = config.getInt("stackable-items." + key);
                stackableItems.put(material, max);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config: " + key);
            }
        }
        getLogger().info("Stackable configuration reloaded: " + stackableItems.size() + " items loaded.");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {
        if ("stackablereload".equalsIgnoreCase(cmd.getName())) {
            if (!sender.hasPermission("stackable.reload")) {
                sender.sendMessage("§cYou don't have permission to reload this config.");
                return true;
            }
            reloadConfiguration();
            sender.sendMessage("§aStackable config reloaded successfully.");
            return true;
        }
        return false;
    }

    private void loadStackableItems() {
        FileConfiguration config = getConfig();
        Set<String> keys = Objects.requireNonNull(config.getConfigurationSection("stackable-items")).getKeys(false);
        for (String key : keys) {
            try {
                Material mat = Material.valueOf(key.toUpperCase());
                int max = config.getInt("stackable-items." + key);
                stackableItems.put(mat, max);
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid material in config: " + key);
            }
        }
    }

    private boolean isStackableItem(Material material) {
        return stackableItems.containsKey(material);
    }

    private int getMaxStackSize(Material material) {
        return stackableItems.getOrDefault(material, material.getMaxStackSize());
    }

    private void applyCustomStackSize(ItemStack item) {
        if (item == null || !isStackableItem(item.getType())) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setMaxStackSize(getMaxStackSize(item.getType()));
            item.setItemMeta(meta);
        }
    }

    // ==== EVENT HANDLERS ====

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent e) {
        scheduler.runTaskAtEntity(e.getWhoClicked(),
                () ->
        applyCustomStackSize(e.getCurrentItem())
                );
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onInventoryDrag(InventoryDragEvent e) {
//        scheduler.runTaskAtEntity(e.getWhoClicked(),
//                () ->
//        e.getNewItems().values().forEach(this::applyCustomStackSize)
//                );
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPickup(EntityPickupItemEvent e) {
        scheduler.runTaskAtEntity(e.getEntity(),
                () ->
        applyCustomStackSize(e.getItem().getItemStack())
        );
    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onPlayerDrop(PlayerDropItemEvent e) {
//        applyCustomStackSize(e.getItemDrop().getItemStack());
//    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onPrepareCraft(PrepareItemCraftEvent e) {
//        ItemStack result = e.getInventory().getResult();
//        if (result != null) {
//            applyCustomStackSize(result);
//            e.getInventory().setResult(result);
//        }
//    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onCraftItem(CraftItemEvent e) {
//        applyCustomStackSize(e.getCurrentItem());
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMove(InventoryMoveItemEvent e) {
        Location location = e.getDestination().getLocation();
        if (location != null) {
            scheduler.runTaskAtLocation(location, () ->
                applyCustomStackSize(e.getItem())
            );
        }
    }
//
//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onInventoryOpen(InventoryOpenEvent e) {
//        fixInventory(e.getInventory());
//    }

//    @EventHandler(priority = EventPriority.LOWEST)
//    public void onInventoryClose(InventoryCloseEvent e) {
//        fixInventory(e.getInventory());
//    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDispense(BlockDispenseEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(),
                () ->
        applyCustomStackSize(e.getItem())
                );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(ItemSpawnEvent e) {
        scheduler.runTaskAtEntity(e.getEntity(), () -> {
            ItemStack item = e.getEntity().getItemStack();
            applyCustomStackSize(item);
            }
        );
    }
//
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreareCrafter(CrafterCraftEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () -> {
                    applyCustomStackSize(e.getResult());
                }
        );
    }
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFurnaceSmelt(FurnaceSmeltEvent e) {
        scheduler.runTaskAtLocation(e.getBlock().getLocation(), () -> {
                    applyCustomStackSize(e.getResult());
                }
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryPickupItem(InventoryPickupItemEvent e) {
        scheduler.runTaskAtLocation(e.getItem().getLocation(), () -> {
                    applyCustomStackSize(e.getItem().getItemStack());
                }
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryCreative(InventoryCreativeEvent e) {
        scheduler.runTaskAtEntity(e.getWhoClicked(), () -> {
                    applyCustomStackSize(e.getCurrentItem());
                }
        );
    }


//
//    @Override
//    public void onDisable() {
//        for (Player p : getServer().getOnlinePlayers()) {
//            fixInventory(p.getInventory());
//        }
//    }

    private void fixInventory(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            applyCustomStackSize(item);
        }
    }
}
