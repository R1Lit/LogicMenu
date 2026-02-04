package pb.r1lit.LogicMenu.lang;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class Lang {
    private final JavaPlugin plugin;
    private FileConfiguration lang;

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File folder = new File(plugin.getDataFolder(), "lang");
        if (!folder.exists()) folder.mkdirs();
        File file = new File(folder, "en_US.yml");
        if (!file.exists()) {
            plugin.saveResource("lang/en_US.yml", false);
        }
        this.lang = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key, String def) {
        if (lang == null) reload();
        String raw = lang.getString(key, def);
        return ChatColor.translateAlternateColorCodes('&', raw == null ? def : raw);
    }

    public String format(String key, String def, Map<String, String> vars) {
        String msg = get(key, def);
        if (vars == null || vars.isEmpty()) return msg;
        String out = msg;
        for (var e : vars.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        return out;
    }
}
