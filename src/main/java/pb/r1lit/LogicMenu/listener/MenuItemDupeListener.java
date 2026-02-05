package pb.r1lit.LogicMenu.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import pb.r1lit.LogicMenu.LogicMenu;
import pb.r1lit.LogicMenu.gui.service.MenuItemMarker;

public class MenuItemDupeListener implements Listener {
    private final LogicMenu plugin;
    private final MenuItemMarker marker;
    private final boolean debug;

    public MenuItemDupeListener(LogicMenu plugin, MenuItemMarker marker, boolean debug) {
        this.plugin = plugin;
        this.marker = marker;
        this.debug = debug;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!marker.isMarked(event.getItem().getItemStack())) {
            return;
        }
        if (debug) {
            plugin.getLogger().info("[anti-dupe] Marked item picked up. Removing.");
        }
        event.getItem().remove();
        event.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!marker.isMarked(event.getItemDrop().getItemStack())) {
            return;
        }
        if (debug) {
            plugin.getLogger().info("[anti-dupe] Marked item dropped. Removing.");
        }
        event.getItemDrop().remove();
        event.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            boolean removedAny = false;
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null) continue;
                if (!marker.isMarked(item)) continue;
                contents[i] = null;
                removedAny = true;
            }
            if (removedAny) {
                player.getInventory().setContents(contents);
                if (debug) {
                    plugin.getLogger().info("[anti-dupe] Removed marked items from " + player.getName());
                }
            }
        }, 10L);
    }
}
