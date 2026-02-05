package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class MenuItemMarker {
    private final NamespacedKey key;
    private final boolean enabled;

    public MenuItemMarker(Plugin plugin, boolean enabled) {
        this.key = new NamespacedKey(plugin, "menu_item");
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ItemStack mark(ItemStack item) {
        if (!enabled || item == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack unmark(ItemStack item) {
        if (!enabled || item == null) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().remove(key);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isMarked(ItemStack item) {
        if (!enabled || item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
