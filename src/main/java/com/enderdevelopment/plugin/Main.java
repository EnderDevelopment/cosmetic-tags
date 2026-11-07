package com.enderdevelopment.plugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Main extends JavaPlugin implements Listener {
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();
    private File playerFile;
    private YamlConfiguration playerData;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        playerFile = new File(getDataFolder(), "players.yml");
        playerData = YamlConfiguration.loadConfiguration(playerFile);
        loadProfiles();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveProfiles();
    }

    private void loadProfiles() {
        for (String id : playerData.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(id);
                String root = id + ".";
                profiles.put(uuid, new Profile(playerData.getStringList(root + "titles"), playerData.getStringList(root + "tags"),
                        playerData.getString(root + "emoji", ""), playerData.getString(root + "color", "#FFFFFF"),
                        playerData.getBoolean(root + "animated", false)));
            } catch (IllegalArgumentException ignored) {
                getLogger().warning("Ignoring invalid player UUID in players.yml: " + id);
            }
        }
    }

    private synchronized void saveProfiles() {
        for (Map.Entry<UUID, Profile> entry : profiles.entrySet()) {
            String root = entry.getKey() + ".";
            Profile profile = entry.getValue();
            playerData.set(root + "titles", profile.titles);
            playerData.set(root + "tags", profile.tags);
            playerData.set(root + "emoji", profile.emoji);
            playerData.set(root + "color", profile.color);
            playerData.set(root + "animated", profile.animated);
        }
        try {
            playerData.save(playerFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save players.yml: " + exception.getMessage());
        }
    }

    private Profile profile(Player player) {
        return profiles.computeIfAbsent(player.getUniqueId(), ignored -> new Profile());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("cosmetic.admin")) {
                sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
                return true;
            }
            reloadConfig();
            sender.sendMessage(Component.text("Cosmetic catalog reloaded.", NamedTextColor.GREEN));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command must be used by a player.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Component.text("/cosmetic title|tag|emoji|color|animation|clear|list", NamedTextColor.GOLD));
            return true;
        }
        Profile p = profile(player);
        String action = args[0].toLowerCase(Locale.ROOT);
        if (action.equals("animation")) {
            p.animated = !p.animated;
            player.sendMessage(Component.text("Animated chat " + (p.animated ? "enabled" : "disabled") + ".", NamedTextColor.GREEN));
            saveProfiles();
            return true;
        }
        if (action.equals("clear")) {
            p.titles.clear(); p.tags.clear(); p.emoji = ""; p.color = "#FFFFFF"; p.animated = false;
            saveProfiles();
            player.sendMessage(Component.text("Your chat cosmetics were cleared.", NamedTextColor.GREEN));
            return true;
        }
        if (action.equals("list")) {
            player.sendMessage(Component.text("Titles: " + String.join(", ", keys("titles")), NamedTextColor.GOLD));
            player.sendMessage(Component.text("Tags: " + String.join(", ", keys("tags")), NamedTextColor.GOLD));
            player.sendMessage(Component.text("Emojis: " + String.join(", ", keys("emojis")), NamedTextColor.GOLD));
            player.sendMessage(Component.text("Colors: " + String.join(", ", keys("colors")), NamedTextColor.GOLD));
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /cosmetic " + action + " <name[,name...]|off>", NamedTextColor.RED));
            return true;
        }
        String value = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        switch (action) {
            case "title", "tag" -> {
                String section = action.equals("title") ? "titles" : "tags";
                List<String> selected = resolveNames(section, value);
                if (selected == null) {
                    player.sendMessage(Component.text("Unknown " + action + ". Use /cosmetic list.", NamedTextColor.RED));
                    return true;
                }
                if (action.equals("title")) p.titles = selected; else p.tags = selected;
                player.sendMessage(Component.text("Equipped " + action + "s: " + (selected.isEmpty() ? "none" : String.join(", ", selected)), NamedTextColor.GREEN));
            }
            case "emoji" -> {
                String found = findKey("emojis", value);
                if (value.equalsIgnoreCase("off")) p.emoji = "";
                else if (found == null) {
                    player.sendMessage(Component.text("Unknown emoji. Use /cosmetic list.", NamedTextColor.RED));
                    return true;
                } else p.emoji = found;
                player.sendMessage(Component.text("Emoji updated.", NamedTextColor.GREEN));
            }
            case "color" -> {
                if (value.equalsIgnoreCase("off")) p.color = "#FFFFFF";
                else {
                    String color = getConfig().getString("colors." + value);
                    if (color == null || TextColor.fromHexString(color) == null) {
                        player.sendMessage(Component.text("Unknown color. Use /cosmetic list.", NamedTextColor.RED));
                        return true;
                    }
                    p.color = color;
                }
                player.sendMessage(Component.text("Chat color updated.", NamedTextColor.GREEN));
            }
            default -> {
                player.sendMessage(Component.text("Unknown option. Use /cosmetic help.", NamedTextColor.RED));
                return true;
            }
        }
        saveProfiles();
        return true;
    }

    private List<String> resolveNames(String section, String input) {
        List<String> result = new ArrayList<>();
        if (input.equalsIgnoreCase("off")) return result;
        for (String part : input.split(",")) {
            String key = findKey(section, part.trim());
            if (key == null) return null;
            if (!result.contains(key)) result.add(key);
        }
        return result;
    }

    private String findKey(String section, String name) {
        ConfigurationSection values = getConfig().getConfigurationSection(section);
        if (values == null) return null;
        return values.getKeys(false).stream().filter(key -> key.equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    private List<String> keys(String section) {
        ConfigurationSection values = getConfig().getConfigurationSection(section);
        return values == null ? List.of() : new ArrayList<>(values.getKeys(false));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Profile p = profiles.get(event.getPlayer().getUniqueId());
        if (p == null) return;
        Component prefix = Component.empty();
        for (String title : p.titles) prefix = prefix.append(styledValue("titles", title)).append(Component.space());
        for (String tag : p.tags) prefix = prefix.append(Component.text("[", NamedTextColor.DARK_GRAY)).append(styledValue("tags", tag)).append(Component.text("] ", NamedTextColor.DARK_GRAY));
        if (!p.emoji.isEmpty()) prefix = prefix.append(styledValue("emojis", p.emoji)).append(Component.space());
        TextColor chatColor = TextColor.fromHexString(p.color);
        if (chatColor == null) chatColor = NamedTextColor.WHITE;
        Component name = Component.text(event.getPlayer().getName(), chatColor);
        if (p.animated) {
            float hue = (System.currentTimeMillis() % 5000L) / 5000.0f;
            name = Component.text(event.getPlayer().getName(), TextColor.color(java.awt.Color.HSBtoRGB(hue, 0.85f, 1.0f) & 0xFFFFFF));
        }
        Component chatPrefix = prefix;
        Component chatName = name;
        TextColor finalChatColor = chatColor;
        event.renderer((source, sourceDisplayName, message, viewer) -> chatPrefix.append(chatName).append(Component.text(": ", NamedTextColor.DARK_GRAY)).append(message.color(finalChatColor)));
    }

    private Component styledValue(String section, String key) {
        String value = getConfig().getString(section + "." + key, key);
        TextColor color = section.equals("emojis") ? NamedTextColor.WHITE : NamedTextColor.GOLD;
        return Component.text(value, color);
    }

    private static final class Profile {
        private volatile List<String> titles = new ArrayList<>();
        private volatile List<String> tags = new ArrayList<>();
        private volatile String emoji = "";
        private volatile String color = "#FFFFFF";
        private volatile boolean animated;
        private Profile() { }
        private Profile(List<String> titles, List<String> tags, String emoji, String color, boolean animated) {
            this.titles = new ArrayList<>(titles); this.tags = new ArrayList<>(tags); this.emoji = emoji; this.color = color; this.animated = animated;
        }
    }
}
