package pb.r1lit.LogicMenu.gui.input;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class AnvilInputManager implements Listener {

    private final Map<UUID, AnvilRequest> requests = new ConcurrentHashMap<>();

    public void open(Player player, String title, String prompt, Consumer<String> onComplete) {
        if (player == null || onComplete == null) return;
        String safePrompt = prompt == null ? "" : ChatColor.translateAlternateColorCodes('&', prompt);

        InventoryView view = openAnvilView(player);
        if (view == null) {
            // Fallback to legacy custom inventory (may not support rename on some servers)
            String safeTitle = colorOr(title, "&8Input");
            Inventory inv = Bukkit.createInventory(new AnvilHolder(player.getUniqueId()), InventoryType.ANVIL, safeTitle);
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                if (!safePrompt.isBlank()) {
                    meta.setDisplayName(safePrompt);
                } else {
                    meta.setDisplayName("");
                }
                paper.setItemMeta(meta);
            }
            inv.setItem(0, paper);
            requests.put(player.getUniqueId(), new AnvilRequest(strip(safePrompt), onComplete, inv));
            player.openInventory(inv);
            return;
        }

        applyTitle(view, title);
        Inventory top = view.getTopInventory();
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null && !safePrompt.isBlank()) {
            meta.setDisplayName(safePrompt);
            paper.setItemMeta(meta);
        }
        top.setItem(0, paper);

        requests.put(player.getUniqueId(), new AnvilRequest(strip(safePrompt), onComplete, top));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        AnvilRequest request = requests.get(player.getUniqueId());
        if (request == null) return;
        if (!isSameTopInventory(event.getView(), request.inventory)) return;

        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        if (event.getRawSlot() != 2) {
            return;
        }

        String cleaned = "";
        String renameText = getRenameText(event.getView(), event.getInventory());
        if (renameText != null && !renameText.isBlank()) {
            cleaned = strip(renameText).trim();
        } else {
            ItemStack result = event.getCurrentItem();
            if (result != null && result.hasItemMeta()) {
                String input = result.getItemMeta().getDisplayName();
                cleaned = strip(input).trim();
            }
        }

        request = requests.remove(player.getUniqueId());
        player.closeInventory();
        if (request == null) return;
        if (cleaned.isBlank() || (!request.prompt.isBlank() && cleaned.equalsIgnoreCase(request.prompt))) {
            return;
        }
        request.onComplete.accept(cleaned);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        AnvilRequest request = requests.get(player.getUniqueId());
        if (request == null) return;
        if (!isSameTopInventory(event.getView(), request.inventory)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        AnvilRequest request = requests.get(player.getUniqueId());
        if (request == null) return;
        if (!isSameTopInventory(event.getView(), request.inventory)) return;
        requests.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        AnvilRequest request = requests.get(player.getUniqueId());
        if (request == null) return;
        if (!isSameTopInventory(event.getView(), request.inventory)) return;

        String renameText = getRenameText(event.getView(), event.getInventory());
        if (renameText == null || renameText.isBlank()) {
            event.setResult(null);
            return;
        }

        ItemStack base = event.getInventory().getItem(0);
        if (base == null) base = new ItemStack(Material.PAPER);
        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(renameText);
            result.setItemMeta(meta);
        }
        event.setResult(result);
        setRepairCost(event.getView(), event.getInventory(), 0);
    }

    private String colorOr(String value, String fallback) {
        String out = value == null || value.isBlank() ? fallback : value;
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    private String strip(String text) {
        return ChatColor.stripColor(text == null ? "" : text);
    }

    private String getRenameText(InventoryView view, Inventory inventory) {
        if (view != null) {
            try {
                var method = view.getClass().getMethod("getRenameText");
                Object value = method.invoke(view);
                if (value instanceof String s) return s;
            } catch (Exception ignored) {
            }
        }
        if (inventory == null) return null;
        try {
            var method = inventory.getClass().getMethod("getRenameText");
            Object value = method.invoke(inventory);
            if (value instanceof String s) return s;
        } catch (Exception ignored) {
        }
        return null;
    }

    private InventoryView openAnvilView(Player player) {
        try {
            return player.openAnvil(null, true);
        } catch (Exception ignored) {
        }
        return null;
    }

    private void applyTitle(InventoryView view, String title) {
        if (view == null) return;
        String safeTitle = colorOr(title, "&8Input");
        try {
            var method = view.getClass().getMethod("setTitle", String.class);
            method.invoke(view, safeTitle);
        } catch (Exception ignored) {
        }
    }

    private void setRepairCost(InventoryView view, Inventory inventory, int cost) {
        try {
            var method = view.getClass().getMethod("setRepairCost", int.class);
            method.invoke(view, cost);
            return;
        } catch (Exception ignored) {
        }
        try {
            var method = inventory.getClass().getMethod("setRepairCost", int.class);
            method.invoke(inventory, cost);
        } catch (Exception ignored) {
        }
    }

    private boolean isSameTopInventory(InventoryView view, Inventory expected) {
        if (view == null || expected == null) return false;
        return view.getTopInventory() == expected;
    }

    private record AnvilRequest(String prompt, Consumer<String> onComplete, Inventory inventory) {}

    private record AnvilHolder(UUID playerId) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
