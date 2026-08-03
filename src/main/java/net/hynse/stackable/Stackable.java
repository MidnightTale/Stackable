package net.hynse.stackable;

import me.nahu.scheduler.wrapper.WrappedScheduler;
import me.nahu.scheduler.wrapper.WrappedSchedulerBuilder;
import net.hynse.stackable.command.ReloadCommand;
import net.hynse.stackable.command.TestRegexCommand;
import net.hynse.stackable.config.ConfigManager;
import net.hynse.stackable.listener.StackableListener;
import net.hynse.stackable.manager.StackSizeManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class Stackable extends JavaPlugin {
    public static Stackable instance;

    private WrappedScheduler scheduler;

    @Override
    public void onEnable() {
        instance = this;
        scheduler = WrappedSchedulerBuilder.builder().plugin(this).build();

        saveDefaultConfig();

        ConfigManager configManager = new ConfigManager();
        configManager.loadConfig();

        StackSizeManager stackSizeManager = new StackSizeManager(configManager);
        StackableListener eventListener = new StackableListener(stackSizeManager, scheduler);

        getServer().getPluginManager().registerEvents(eventListener, this);
        eventListener.startPermissionRefreshMonitor();

        Objects.requireNonNull(getCommand("stackablereload")).setExecutor(
                new ReloadCommand(configManager, stackSizeManager, eventListener, scheduler)
        );

        TestRegexCommand testRegexCommand = new TestRegexCommand();
        Objects.requireNonNull(getCommand("stackabletest")).setExecutor(testRegexCommand);
        Objects.requireNonNull(getCommand("stackabletest")).setTabCompleter(testRegexCommand);

        getLogger().info("Stackable plugin enabled with "
                + configManager.getDirectMaterialCount() + " base materials, "
                + configManager.getWildcardPatternCount() + " wildcards, and "
                + configManager.getGroupCount() + " groups.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        getLogger().info("Stackable plugin disabled.");
        instance = null;
    }
}
