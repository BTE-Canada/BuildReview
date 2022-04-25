package io.github.celery6.buildreview2;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class onPlayerJoin implements Listener {
    private static final Plugin plugin = BuildReview2.getPlugin(BuildReview2.class);
    FileConfiguration config = plugin.getConfig();

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        if (e.getPlayer().hasPermission( "buildreview.complete")) {
            final ConfigurationSection section = config.getConfigurationSection("br.pending");
            if ((section.getKeys(false).isEmpty())) {
            } else {
                e.getPlayer().sendMessage(ChatColor.GOLD + "---------------------THERE ARE PENDING BUILDS YOU SHOULD REVIEW RIGHT NOW!-------------------");
                for(String name : section.getKeys(false)) {
                    final String time = config.getStringList("br.pending." + name).get(7);
                    e.getPlayer().sendMessage(ChatColor.BLUE + name  + ChatColor.GRAY + " | " + time);
                }
            }

        }
    }
}
