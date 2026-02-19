package pb.r1lit.LogicMenu.meta;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MetaStore {
    private final JavaPlugin plugin;
    private final File folder;
    private final Map<UUID, FileConfiguration> cache = new ConcurrentHashMap<>();
    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private int taskId = -1;

    public MetaStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "meta");
        if (!folder.exists()) folder.mkdirs();
        startAutoSave();
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
        dirty.add(player.getUniqueId());
    }

    public void remove(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        cfg.set(key, null);
        dirty.add(player.getUniqueId());
    }

    public void increment(Player player, String key, int delta) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        int current = cfg.getInt(key, 0);
        cfg.set(key, current + delta);
        dirty.add(player.getUniqueId());
    }

    public void toggle(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return;
        FileConfiguration cfg = load(player.getUniqueId());
        String cur = cfg.getString(key, "false");
        boolean val = "true".equalsIgnoreCase(cur) || "1".equals(cur);
        cfg.set(key, val ? "false" : "true");
        dirty.add(player.getUniqueId());
    }

    /**
     * Flush all dirty entries to disk asynchronously.
     */
    public void flushAsync() {
        if (dirty.isEmpty()) return;
        // Snapshot dirty UUIDs and their configs
        Map<UUID, String> toSave = new ConcurrentHashMap<>();
        for (UUID uuid : dirty) {
            FileConfiguration cfg = cache.get(uuid);
            if (cfg != null) {
                toSave.put(uuid, cfg.saveToString());
            }
        }
        dirty.clear();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Map.Entry<UUID, String> entry : toSave.entrySet()) {
                try {
                    File file = new File(folder, entry.getKey() + ".yml");
                    java.nio.file.Files.writeString(file.toPath(), entry.getValue());
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save meta for " + entry.getKey() + ": " + e.getMessage());
                }
            }
        });
    }

    /**
     * Flush all dirty entries synchronously. Call on plugin disable.
     */
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (UUID uuid : dirty) {
            FileConfiguration cfg = cache.get(uuid);
            if (cfg == null) continue;
            try {
                File file = new File(folder, uuid + ".yml");
                cfg.save(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save meta for " + uuid + ": " + e.getMessage());
            }
        }
        dirty.clear();
    }

    private void startAutoSave() {
        // Flush every 60 seconds (1200 ticks)
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::flushAsync, 1200L, 1200L).getTaskId();
    }

    private FileConfiguration load(UUID uuid) {
        return cache.computeIfAbsent(uuid, id -> {
            File file = new File(folder, id + ".yml");
            return YamlConfiguration.loadConfiguration(file);
        });
    }
}
