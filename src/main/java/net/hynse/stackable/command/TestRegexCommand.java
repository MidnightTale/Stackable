package net.hynse.stackable.command;

import net.hynse.stackable.Stackable;
import net.hynse.stackable.util.RegexUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command to test regex patterns against material names
 */
public class TestRegexCommand implements CommandExecutor, TabCompleter {

    /**
     * Executes the command
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stackable.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /stackabletest <pattern> [material]");
            sender.sendMessage(ChatColor.YELLOW + "Examples:");
            sender.sendMessage(ChatColor.YELLOW + "  /stackabletest .*_BOAT - Test a pattern against all materials");
            sender.sendMessage(ChatColor.YELLOW + "  /stackabletest .*WOOL WHITE_WOOL - Test if pattern matches specific material");
            return true;
        }

        String pattern = args[0];
        
        // If a specific material is provided, test only that material
        if (args.length > 1) {
            try {
                String materialName = args[1].toUpperCase();
                Material material = Material.valueOf(materialName);
                boolean matches = RegexUtil.testPattern(pattern, material.name());
                
                if (matches) {
                    sender.sendMessage(ChatColor.GREEN + "Pattern '" + pattern + "' MATCHES material '" + material.name() + "'");
                } else {
                    sender.sendMessage(ChatColor.RED + "Pattern '" + pattern + "' does NOT match material '" + material.name() + "'");
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid material name: " + args[1]);
            }
            return true;
        }
        
        // If no specific material is provided, test against all materials and show matches
        List<String> matches = new ArrayList<>();
        int count = 0;
        
        for (Material material : Material.values()) {
            if (RegexUtil.testPattern(pattern, material.name())) {
                matches.add(material.name());
                count++;
                
                // Limit the number of matches shown to avoid spamming
                if (matches.size() >= 15) {
                    break;
                }
            }
        }
        
        if (count > 0) {
            sender.sendMessage(ChatColor.GREEN + "Pattern '" + pattern + "' matches " + count + " materials.");
            sender.sendMessage(ChatColor.GREEN + "First " + Math.min(count, 15) + " matches: " + 
                    String.join(", ", matches));
            
            if (count > 15) {
                sender.sendMessage(ChatColor.GREEN + "...and " + (count - 15) + " more.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Pattern '" + pattern + "' does not match any materials.");
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            String partialMaterial = args[1].toUpperCase();
            return Arrays.stream(Material.values())
                    .map(Material::name)
                    .filter(name -> name.startsWith(partialMaterial))
                    .limit(20)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
