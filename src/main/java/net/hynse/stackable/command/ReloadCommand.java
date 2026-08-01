package net.hynse.stackable.command;

import net.hynse.stackable.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;
    
    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("stackable.reload") && !sender.hasPermission("stackable.admin")) {
            sender.sendMessage("§cYou don't have permission to reload this config.");
            return true;
        }
        
        configManager.reloadConfig();
        sender.sendMessage("§aStackable config reloaded successfully.");
        return true;
    }
}