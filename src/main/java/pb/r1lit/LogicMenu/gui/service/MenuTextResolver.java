package pb.r1lit.LogicMenu.gui.service;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pb.r1lit.LogicMenu.LogicMenu;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MenuTextResolver {
    private static final Pattern META_PATTERN = Pattern.compile("%logicmenu_meta_([A-Za-z0-9_.-]+)%");

    public String resolve(String text, Player player, Map<String, String> vars) {
        if (text == null) return "";
        String resolved = text;
        if (vars != null) {
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        if (player != null) {
            LogicMenu plugin = LogicMenu.getInstance();
            if (plugin != null && plugin.getMetaStore() != null) {
                Matcher matcher = META_PATTERN.matcher(resolved);
                StringBuffer buffer = new StringBuffer();
                while (matcher.find()) {
                    String key = matcher.group(1);
                    String value = plugin.getMetaStore().get(player, key);
                    matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
                }
                matcher.appendTail(buffer);
                resolved = buffer.toString();
            }
        }
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            resolved = PlaceholderAPI.setPlaceholders(player, resolved);
        }
        return color(resolved);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}

