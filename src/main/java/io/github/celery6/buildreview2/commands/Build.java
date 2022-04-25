package io.github.celery6.buildreview2.commands;

import io.github.celery6.buildreview2.BuildReview2;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.omg.CORBA.PRIVATE_MEMBER;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class Build implements CommandExecutor {
    private static final Plugin plugin = BuildReview2.getPlugin(BuildReview2.class);
    private final BuildReview2 br = BuildReview2.getInstance();
    FileConfiguration config = plugin.getConfig();
    JDA bot = BuildReview2.jda;
    Connection conn = BuildReview2.conn;


    private String getDiscord(String uuid) {
        try {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * from discordsrv_accounts where uuid = '" + uuid + "'");
            rs.next();
            return rs.getString("discord");
        } catch(SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        final Player player = (Player) sender;

        if (player.hasPermission( "buildreview.no")) {
            sender.sendMessage(ChatColor.RED + "You're not a trial builder anymore!");
            return true;
        }
        if(player.hasPermission("buildreview.visitor")){
            sender.sendMessage(ChatColor.RED + "You're not a trial builder yet!");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Use /build help to view all commands.");
            return true;
        }
        final String arg0 = args[0].toLowerCase();

        if (arg0.equals("help")) {
            FileConfiguration what = br.getConfig();

            if (player.hasPermission("buildreview.complete")) {
                sender.sendMessage(ChatColor.GOLD + "---------------------Buildreview help-------------------");
                sender.sendMessage(ChatColor.GREEN + "/build help" + ChatColor.BLUE + " = This page");
                sender.sendMessage(ChatColor.GREEN + "/build submit" + ChatColor.BLUE + " = Submit your build to be reviewed");
                sender.sendMessage(ChatColor.GREEN + "/build cancel" + ChatColor.BLUE + " = Cancel your build submission");
                sender.sendMessage(ChatColor.GREEN + "/build list <P | A | D>" + ChatColor.BLUE + " = List builds by their statuses (Pending | Accepted | Declined");
                sender.sendMessage(ChatColor.GREEN + "/build goto <username>" + ChatColor.BLUE + " = Teleport to a submitted build");
                sender.sendMessage(ChatColor.GREEN + "/build complete <username>" + ChatColor.BLUE + " = Accept a build");
                sender.sendMessage(ChatColor.GREEN + "/build deny <username>" + ChatColor.BLUE + " = Decline a build");
            } else {
                sender.sendMessage(ChatColor.GOLD + "---------------------Buildreview help-------------------");
                sender.sendMessage(ChatColor.GREEN + "/build help" + ChatColor.BLUE + " = This page");
                sender.sendMessage(ChatColor.GREEN + "/build submit" + ChatColor.BLUE + " = Submit your build to be reviewed");
            }
            return true;
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.CANADA).withZone(ZoneId.of("America/Montreal"));
        final Instant instant = Instant.now();
        final String dateTime = formatter.format(instant);
        final Location location = player.getLocation();
        final TextChannel general = bot.getTextChannelById("692799700985970719");
        final TextChannel reviewers = bot.getTextChannelById("858028703723290634");

        //---------------- BUILD SUBMIT ----------------
        if (arg0.equals("submit") && player.hasPermission("group.trialbuilder")) {
            final String name = player.getName().toLowerCase();
            final String playerUuid = player.getUniqueId().toString();

            if (config.getString("br.pending." + name) != null) {
                sender.sendMessage(ChatColor.RED + "You already submitted your trial build!");
                return true;
            }

            /*if (config.getString("br.denied." + name) != null) {*/
                config.set("br.denied." + name, null);

            List<String> inputList = Arrays.asList(location.getWorld().getName(), String.valueOf(location.getX()), String.valueOf( location.getY()), String.valueOf( location.getZ()), String.valueOf(location.getYaw()), String.valueOf(location.getPitch()), playerUuid, dateTime);
            config.set("br.pending." + name, inputList);
            plugin.saveConfig();

            sender.sendMessage(ChatColor.GOLD + "You submitted your trial build! Wait patiently for a staff member to review your trial build (may take a few hours or a day). You'll be notified on Discord once it's reviewed.");
            Bukkit.broadcast(ChatColor.BLUE + "[Build Submit] " + ChatColor.GOLD + name + " has submitted their build for review. UUID: " + playerUuid, "buildreview.complete");

            final String discord = getDiscord(playerUuid);
            reviewers.sendMessage(name + " (<@" + discord + ">) submitted a build! <@&825831754312712192>").complete();
            return true;
        } else if ((arg0.equals("goto") || arg0.equals("deny") || arg0.equals("complete")) && player.hasPermission("buildreview.complete")) {

            ////---------------- GOTO/DECLINE/COMPLETE ----------------
            if(args.length < 2) {
                sender.sendMessage(ChatColor.RED + "You did not specify a username.");
                return true;
            }

            final String builder = args[1].toLowerCase();
            if (!(config.contains("br.pending." + builder))) {
                sender.sendMessage(ChatColor.RED + builder + " has not submitted a build.");
                return true;
            }

            final String reviewer = player.getName();
            final List<String> pendingList = config.getStringList("br.pending." + builder);

            //---------------- BUILD GOTO ----------------
            if (arg0.equals("goto")) {
                final Location loc2 = new Location(Bukkit.getWorld(pendingList.get(0)), Double.parseDouble(pendingList.get(1)) , Double.parseDouble(pendingList.get(2)), Double.parseDouble(pendingList.get(3)),Float.parseFloat(pendingList.get(4)) ,Float.parseFloat(pendingList.get(5)));
                player.teleport(loc2);
                sender.sendMessage(ChatColor.GRAY + "Teleporting you to " + builder + "'s plot!");
                sender.sendMessage(ChatColor.GRAY + "To confirm you have been do " + ChatColor.GREEN + "/build <complete|deny> <username>");
                return true;
            }

            final String builderDiscord = getDiscord(pendingList.get(6));
            User user = bot.retrieveUserById(builderDiscord).complete();

            //---------------- BUILD DENY ----------------
            if (arg0.equals("deny")) {
                EmbedBuilder denyEmbed = new EmbedBuilder();
                denyEmbed.setTitle("<a:SPINNYCANADA:854075968096698398> Your trial build has been reviewed. <a:SPINNYCANADA:854075968096698398>");
                denyEmbed.setDescription("Your trial build submission has been reviewed and declined (pretty much everyone gets declined at least once: we want to encourage learning and improvement, so don't feel discouraged). Hop on the server and improve your build based on the feedback there, then resubmit it!\n\nIf you have any questions or concerns, feel free to ask in our discord server.");
                config.set("br.pending." + builder, null);
                config.set("br.denied." + builder, pendingList);
                plugin.saveConfig();

                sender.sendMessage(ChatColor.RED + builder + "'s trial build has been declined.");
                user.openPrivateChannel()
                        .flatMap(channel -> channel.sendMessage("_ _").setEmbeds(denyEmbed.build()))
                        .queue();

                reviewers.sendMessage(builder + " (<@" + builderDiscord + ">) got rejected! Reviewer: " + reviewer).complete();
                return true;
            }

            //---------------- BUILD COMPLETE ----------------
            EmbedBuilder completeEmbed2 = new EmbedBuilder();
            completeEmbed2.setTitle("*Optional step:*");
            completeEmbed2.setDescription("To become an \"official\" BTE builder (you'll get builder role in main BTE discord, but nothing else changes), fill out [this form](https://buildtheearth.net/bte-canada).");
            EmbedBuilder completeEmbed = new EmbedBuilder();
            completeEmbed.setTitle("**Your trial build was approved. Congrats! You're now a Novice Builder! <a:crabrave:696890020056924233> <a:SPINNYCANADA:854075968096698398>**");
            completeEmbed.setDescription("__With the **Novice Builder** rank, you can now build **Small Builds!**__\n" +
                    "Examples: residential houses, low-rise commercial buildings around 1-3 floors tall (like Tim Horton's) anywhere in :flag_ca: Canada :flag_ca: .\n" +
                    "\n" +
                    "Make sure you look through <#928017130971070544> to learn how you can submit builds to gain points and **RANK UP** to unlock more epic buildings!\n" +
                    "\n" +
                    "Remember to use tpll (*never use the default brick outlines cuz they are terribly inaccurate!*), and update your building progress on our [progress map](https://discord.com/channels/692799601983488021/821890511760654366/857475153449058315). Have fun building CANADA! ");

            config.set("br.pending." + builder, null);
            config.set("br.complete." + builder, pendingList);
            plugin.saveConfig();

            Permission perm = BuildReview2.getInstance().getPermissions();
            OfflinePlayer newBuilder = Bukkit.getOfflinePlayer(UUID.fromString(pendingList.get(6)));
            perm.playerAddGroup(null, newBuilder,"novicebuilder");
            perm.playerRemoveGroup(null, newBuilder, "trialbuilder");

            bot.getGuildById("692799601983488021").addRoleToMember(builderDiscord, bot.getRoleById("692801758761844746")).queue();
            bot.getGuildById("692799601983488021").removeRoleFromMember(builderDiscord, bot.getRoleById("692802742200172634")).queue();

            try {
                conn.createStatement().executeUpdate("insert into points.players (user_id, uuid) values ('" + builderDiscord + "', '" + pendingList.get(6)+ "')");
            } catch(SQLException e) {
                e.printStackTrace();
            }

            Bukkit.broadcastMessage(ChatColor.GREEN + "NEW BUILDER ALERT! " + ChatColor.DARK_GREEN + builder + ChatColor.GREEN + " is now a BTE Canada builder. Woohoo!");
            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessage("_ _").setEmbeds(completeEmbed.build(), completeEmbed2.build()))
                    .queue();

            general.sendMessage(builder + "<a:crabrave:696890020056924233> (<@" + builderDiscord + ">) is now a builder! YAY! <a:crabrave:696890020056924233>").complete();

            reviewers.sendMessage(builder + " (<@" + builderDiscord + ">) is now a builder! YAY! Reviewer: " + reviewer).complete();

            sender.sendMessage(ChatColor.GREEN + builder + "'s trial build has been approved.");
            return true;
        } else if (arg0.equals("list") && player.hasPermission("buildreview.complete")) {
            //---------------- /BUILD LIST ----------------
            if(args.length < 2 ) {
                sender.sendMessage(ChatColor.RED + "You did not specify a state (Pending/Approved/Declined). Correct command usage: " + ChatColor.GREEN + "/build list <P|A|D>");
                return true;
            }
            if (args[1].equalsIgnoreCase("p")) {
                final ConfigurationSection section = config.getConfigurationSection("br.pending");
                if (section == null) {
                    sender.sendMessage(ChatColor.BLUE + "There are no pending builds. SO COOL!");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "Pending:");
                for(String name : section.getKeys(false)) {
                    final String time = config.getStringList("br.pending." + name).get(7);
                    sender.sendMessage(ChatColor.BLUE + name  + ChatColor.GRAY + " | " + time);
                }
            } else if (args[1].equalsIgnoreCase("a")) {
                final ConfigurationSection section = config.getConfigurationSection("br.complete");
                if (section == null) {
                    sender.sendMessage(ChatColor.BLUE + "There are no approved builds. wtf? something must have gone wrong.");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "Approved:");
                for(String name : section.getKeys(false)) {
                    final String time = config.getStringList("br.complete." + name).get(7);
                    sender.sendMessage(ChatColor.GREEN + name  + ChatColor.GRAY + " | " + time);
                }
            } else if (args[1].equalsIgnoreCase("d")) {
                final ConfigurationSection section = config.getConfigurationSection("br.denied");
                if (section == null) {
                    sender.sendMessage(ChatColor.RED + "There are no declined builds.");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "Declined:");
                for(String name : section.getKeys(false)) {
                    final String time = config.getStringList("br.denied." + name).get(7);
                    sender.sendMessage(ChatColor.GREEN + name  + ChatColor.GRAY + " | " + time);
                }
            }
        }
        return true;
    }
}
