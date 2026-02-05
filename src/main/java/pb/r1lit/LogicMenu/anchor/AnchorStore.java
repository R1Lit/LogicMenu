package pb.r1lit.LogicMenu.anchor;

import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AnchorStore {
    private final Map<UUID, Map<String, String>> store = new ConcurrentHashMap<>();

    public void set(Player player, String key, String value) {
        if (player == null || key == null || key.isBlank()) return;
        store.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>())
                .put(key, value == null ? "" : value);
    }

    public String get(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return "";
        Map<String, String> map = store.get(player.getUniqueId());
        if (map == null) return "";
        return map.getOrDefault(key, "");
    }

    public Map<String, String> getAll(Player player) {
        if (player == null) return Map.of();
        Map<String, String> map = store.get(player.getUniqueId());
        if (map == null || map.isEmpty()) return Map.of();
        return Collections.unmodifiableMap(map);
    }

    public void remove(Player player, String key) {
        if (player == null || key == null || key.isBlank()) return;
        Map<String, String> map = store.get(player.getUniqueId());
        if (map != null) map.remove(key);
    }

    public void clear(Player player) {
        if (player == null) return;
        store.remove(player.getUniqueId());
    }
}
