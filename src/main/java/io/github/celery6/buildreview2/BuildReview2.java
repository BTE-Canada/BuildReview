package io.github.celery6.buildreview2;

import io.github.celery6.buildreview2.onPlayerJoin;
import io.github.celery6.buildreview2.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;

public class BuildReview2 extends JavaPlugin {
    public static Connection conn;
    public static JDA jda;
    private static Permission perms;
    private static BuildReview2 instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FileConfiguration config = this.getConfig();
        try {
            conn = DriverManager.getConnection(config.getString("db.jdbcUrl"), config.getString("db.user"), config.getString("db.pass"));
            jda = JDABuilder.createDefault(config.getString("bot.token")).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getServer().getPluginManager().registerEvents(new onPlayerJoin(), this);
        this.getCommand("build").setExecutor(new Build());
        this.setupPermissions();

        getLogger().info("BUILDREVIEW HAS BEEN ENABLED AAAAHHHHH!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BUILDREVIEW died");
    }

    private boolean setupPermissions() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp == null) {
            return false;
        }
        perms = rsp.getProvider();
        return perms != null;
    }

    public Permission getPermissions() {
        return perms;
    }
    public static BuildReview2 getInstance() {
        return instance;
    }
}
