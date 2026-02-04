package pb.r1lit.LogicMenu.command;

import org.bukkit.plugin.PluginDescriptionFile;

import java.io.File;
import java.util.Optional;
import java.util.jar.JarFile;

public final class PluginJarUtil {
    private PluginJarUtil() {
    }

    public static Optional<String> readPluginName(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            var entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) return Optional.empty();
            try (var in = jarFile.getInputStream(entry)) {
                PluginDescriptionFile desc = new PluginDescriptionFile(in);
                return Optional.ofNullable(desc.getName());
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
