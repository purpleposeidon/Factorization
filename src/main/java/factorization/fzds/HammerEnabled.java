package factorization.fzds;

import factorization.coremod.LoadingPlugin;
import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class HammerEnabled {
    public static final boolean ENABLED = isEnabled();

    private static boolean isEnabled() {
        final File configDirectory = new File(LoadingPlugin.getMcLocation(), "config");
        if (!configDirectory.exists()) return true;
        File cfgName = new File(configDirectory, "hammerChannels.cfg");
        Configuration config = new Configuration(cfgName);
        boolean ret = config.getBoolean("enabled", "hammer", true, "Set to false to disable FZDS. Setting to false will disable colossi, hinges, twisted blocks, etc.");
        if (config.hasChanged()) {
            config.save();
        }
        return ret;
    }
}
