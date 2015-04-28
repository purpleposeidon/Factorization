package factorization.fzds;

import factorization.coremod.LoadingPlugin;
import factorization.shared.Core;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class HammerEnabled {
    public static final boolean ENABLED = isEnabled();

    private static boolean isEnabled() {
        final String the_devil = "Bukkit, Craftbukkit, Cauldron, or MCPC+";
        boolean init = true;
        try {
            Class cl = Class.forName("org.bukkit.Bukkit");
            if (cl != null) {
                init = Boolean.parseBoolean(System.getProperty("fz.hammer.forceEnableWithBukkit", "false"));
            }
            if (!init) {
                Core.logWarning(the_devil + " detected; disabling Hammer");
            }
        } catch (ClassNotFoundException e) {
            // No bukkit
        }

        final File configDirectory = new File(LoadingPlugin.getMcLocation(), "config");
        if (!configDirectory.exists()) return init;
        File cfgName = new File(configDirectory, "hammerChannels.cfg");
        Configuration config = new Configuration(cfgName);
        String comment = "Set to false to disable FZDS. Setting to false will disable colossi, hinges, twisted blocks, etc.";
        if (!init) {
            comment = "Force-disabled by " + the_devil + "; setting to true will do nothing.";
        }
        boolean ret = config.getBoolean("enabled", "hammer", init, comment);
        if (!init) {
            ret = false;
        }
        if (config.hasChanged()) {
            config.save();
        }
        return ret;
    }
}
