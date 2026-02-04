package pb.r1lit.LogicMenu.meta;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MetaStore {
    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, FileConfiguration> cache = new ConcurrentHashMap<>();

    public MetaStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "meta");
        if (!folder.exists()) folder.mkdirs();
    }

    public String get(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return "";
        FileConfiguration cfg = load(player.getUniqueId());
        return cfg.getString(key, "");
    }

    public void set(Player player, String key, String value) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        cfg.set(key, value == null ? "" : value);
        save(player.getUniqueId(), cfg);
    }

    public void remove(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        cfg.set(key, null);
        save(player.getUniqueId(), cfg);
    }

    public void increment(Player player, String key, int delta) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        int current = cfg.getInt(key, 0);
        cfg.set(key, current + delta);
        save(player.getUniqueId(), cfg);
    }

    public void toggle(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        String cur = cfg.getString(key, "false");
        boolean val = "true".equalsIgnoreCase(cur) || "1".equals(cur);
        cfg.set(key, val ? "false" : "true");
        save(player.getUniqueId(), cfg);
    }

    private FileConfiguration load(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File file = new File(folder, id + ".yml");
            return YamlConfiguration.loadConfiguration(file);
        });
    }

    private void save(UUID uuid, FileConfiguration cfg) {
        try {
            File file = new File(folder, uuid + ".yml");
            cfg.save(file);
        } catch (Exception ignored) {
        }
    }
}
