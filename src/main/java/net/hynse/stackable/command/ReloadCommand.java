package net.hynse.stackable.command;

import me.nahu.scheduler.wrapper.WrappedScheduler;
import net.hynse.stackable.config.ConfigManager;
import net.hynse.stackable.listener.StackableListener;
import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;
    private final StackSizeManager stackSizeManager;
    private final StackableListener listener;
    private final WrappedScheduler scheduler;

    public ReloadCommand(
            ConfigManager configManager,
            StackSizeManager stackSizeManager,
            StackableListener listener,
            WrappedScheduler scheduler
    ) {
        this.configManager = configManager;
        this.stackSizeManager = stackSizeManager;
        this.listener = listener;
        this.scheduler = scheduler;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("stackable.reload")
                && !sender.hasPermission("stackable.admin")) {
            sender.sendMessage("§cYou don't have permission to reload this config.");
            return true;
        }

        configManager.reloadConfig();

        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        for (Player player : Bukkit.getOnlinePlayers()) {
            scheduler.runTaskAtEntity(player, () -> {
                stackSizeManager.refreshPlayer(player);
                listener.rememberPermissionState(player);
            });
        }

        sender.sendMessage("§aStackable config reloaded; refreshing "
                + onlinePlayers + " online player(s).");
        return true;
    }
}
