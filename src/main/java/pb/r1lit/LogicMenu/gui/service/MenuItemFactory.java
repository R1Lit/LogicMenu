package pb.r1lit.LogicMenu.gui.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pb.r1lit.LogicMenu.gui.model.MenuItemDefinition;
import pb.r1lit.LogicMenu.gui.service.MenuItemMarker;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MenuItemFactory {

    private final MenuTextResolver resolver;
    private final MenuItemMarker marker;

    public MenuItemFactory(MenuTextResolver resolver, MenuItemMarker marker) {
        this.resolver = resolver;
        this.marker = marker;
    }

    public ItemStack buildItem(MenuItemDefinition def, org.bukkit.entity.Player player, Map<String, String> vars, Map<String, String> extraVars) {
        if (def == null) return null;
        java.util.Map<String, String> allVars = new java.util.HashMap<>(vars == null ? java.util.Map.of() : vars);
        if (extraVars != null) allVars.putAll(extraVars);

        String rawMat = resolver.resolve(def.getMaterial(), player, allVars);

        ItemStack external = tryBuildExternalItem(rawMat);
        if (external != null) {
            applyMeta(external, def, player, allVars);
            external.setAmount(Math.max(1, def.getAmount()));
            if (marker != null) marker.mark(external);
            return external;
        }

        String matName = rawMat.toUpperCase(Locale.ROOT);
        String lowerMat = rawMat.toLowerCase(Locale.ROOT);
        Material material;
        boolean isHead = false;
        String headOwner = null;
        String headBase64 = null;
        String headTextureId = null;
        String headTextureUrl = null;

        if (lowerMat.startsWith("head;") || lowerMat.startsWith("head-") || lowerMat.startsWith("head:")) {
            isHead = true;
            headOwner = rawMat.substring(5).trim();
            material = Material.PLAYER_HEAD;
        } else if (lowerMat.startsWith("basehead-") || lowerMat.startsWith("base64-") || lowerMat.startsWith("b64-")
                || lowerMat.startsWith("headbase64-") || lowerMat.startsWith("head64-")
                || lowerMat.startsWith("basehead:") || lowerMat.startsWith("base64:") || lowerMat.startsWith("b64:")
                || lowerMat.startsWith("headbase64:") || lowerMat.startsWith("head64:")) {
            isHead = true;
            material = Material.PLAYER_HEAD;
            headBase64 = stripHeadPrefix(rawMat, lowerMat);
        } else if (lowerMat.startsWith("texture-") || lowerMat.startsWith("texture:") ||
                lowerMat.startsWith("textureid-") || lowerMat.startsWith("textureid:")) {
            isHead = true;
            material = Material.PLAYER_HEAD;
            headTextureId = stripHeadPrefix(rawMat, lowerMat);
        } else if (lowerMat.startsWith("headurl-") || lowerMat.startsWith("headurl:")
                || lowerMat.startsWith("textureurl-") || lowerMat.startsWith("textureurl:")
                || lowerMat.startsWith("url-") || lowerMat.startsWith("url:")) {
            isHead = true;
            material = Material.PLAYER_HEAD;
            headTextureUrl = stripHeadPrefix(rawMat, lowerMat);
        } else {
            try {
                material = Material.valueOf(matName);
            } catch (IllegalArgumentException e) {
                material = Material.STONE;
            }
        }

        ItemStack item = new ItemStack(material, Math.max(1, def.getAmount()));
        applyMeta(item, def, player, allVars);

        if (isHead) {
            ItemMeta m = item.getItemMeta();
            if (m instanceof SkullMeta skullMeta) {
                if (headOwner != null && !headOwner.isBlank()) {
                    skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(headOwner));
                } else if (headBase64 != null && !headBase64.isBlank()) {
                    applyBase64ToSkull(skullMeta, headBase64);
                } else if (headTextureId != null && !headTextureId.isBlank()) {
                    String base64 = textureToBase64(headTextureId);
                    applyBase64ToSkull(skullMeta, base64);
                } else if (headTextureUrl != null && !headTextureUrl.isBlank()) {
                    String base64 = urlToBase64(headTextureUrl);
                    applyBase64ToSkull(skullMeta, base64);
                }
                item.setItemMeta(skullMeta);
            }
        }

        if (marker != null) marker.mark(item);
        return item;
    }

    private void applyMeta(ItemStack item, MenuItemDefinition def, org.bukkit.entity.Player player, Map<String, String> vars) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(resolver.resolve(def.getName(), player, vars));
            List<String> lore = new ArrayList<>();
            for (String line : def.getLore()) {
                lore.add(resolver.resolve(line, player, vars));
            }
            meta.setLore(lore);
            if (def.getModelData() != null) {
                meta.setCustomModelData(def.getModelData());
            }
            if (def.isUnbreakable()) {
                meta.setUnbreakable(true);
            }
            if (def.isHideAttributes()) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE,
                        ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ARMOR_TRIM);
            }
            if (def.getItemFlags() != null) {
                for (String flagName : def.getItemFlags()) {
                    try {
                        ItemFlag flag = ItemFlag.valueOf(flagName.trim().toUpperCase(Locale.ROOT));
                        meta.addItemFlags(flag);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            item.setItemMeta(meta);
        }

        if (def.isGlow()) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            ItemMeta m = item.getItemMeta();
            if (m != null) {
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(m);
            }
        }
        if (def.getEnchantments() != null) {
            for (String ench : def.getEnchantments()) {
                String[] parts = ench.split(":");
                String enchName = parts[0].trim().toUpperCase(Locale.ROOT);
                int level = parts.length > 1 ? parseIntSafe(parts[1].trim()) : 1;
                Enchantment e = Enchantment.getByName(enchName);
                if (e != null) {
                    item.addUnsafeEnchantment(e, Math.max(1, level));
                }
            }
        }
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private ItemStack tryBuildExternalItem(String rawMat) {
        String mat = rawMat.toLowerCase(Locale.ROOT);
        if (mat.startsWith("itemsadder-") || mat.startsWith("itemsadder:")) {
            String id = rawMat.substring(rawMat.indexOf('-') + 1).trim();
            if (rawMat.contains(":")) id = rawMat.substring(rawMat.indexOf(':') + 1).trim();
            try {
                Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
                Object instance = customStack.getMethod("getInstance", String.class).invoke(null, id);
                if (instance != null) {
                    return (ItemStack) customStack.getMethod("getItemStack").invoke(instance);
                }
            } catch (Throwable ignored) {
            }
        }

        if (mat.startsWith("oraxen-") || mat.startsWith("oraxen:")) {
            String id = rawMat.substring(rawMat.indexOf('-') + 1).trim();
            if (rawMat.contains(":")) id = rawMat.substring(rawMat.indexOf(':') + 1).trim();
            try {
                Class<?> oraxenItems = Class.forName("io.th0rgal.oraxen.api.OraxenItems");
                Object itemBuilder = oraxenItems.getMethod("getItemById", String.class).invoke(null, id);
                if (itemBuilder != null) {
                    return (ItemStack) itemBuilder.getClass().getMethod("build").invoke(itemBuilder);
                }
            } catch (Throwable ignored) {
            }
        }

        if (mat.startsWith("hdb-")) {
            String id = rawMat.substring("hdb-".length()).trim();
            try {
                Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
                Object api = apiClass.getConstructor().newInstance();
                return (ItemStack) apiClass.getMethod("getItemHead", String.class).invoke(api, id);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private void applyBase64ToSkull(SkullMeta meta, String base64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass
                    .getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), null);

            Object properties = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);
            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception ignored) {
        }
    }

    private String textureToBase64(String textureId) {
        String url = "https://textures.minecraft.net/texture/" + textureId;
        return urlToBase64(url);
    }

    private String urlToBase64(String url) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private String stripHeadPrefix(String raw, String lowerRaw) {
        String[] prefixes = {
                "head;", "head-", "head:",
                "basehead-", "base64-", "b64-", "headbase64-", "head64-",
                "basehead:", "base64:", "b64:", "headbase64:", "head64:",
                "texture-", "texture:", "textureid-", "textureid:",
                "headurl-", "headurl:", "textureurl-", "textureurl:", "url-", "url:"
        };
        for (String prefix : prefixes) {
            if (lowerRaw.startsWith(prefix)) {
                return raw.substring(prefix.length()).trim();
            }
        }
        return raw;
    }
}

