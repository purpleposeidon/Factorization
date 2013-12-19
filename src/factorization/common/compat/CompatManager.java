package factorization.common.compat;

import cpw.mods.fml.common.Loader;
import factorization.shared.Core;

public class CompatManager {
    String[] mod_compats = new String[] {"IC2", "Thaumcraft"};
    
    String base_name = getClass().getCanonicalName().replace(getClass().getSimpleName(), "");
    
    public void loadCompat() {
        ClassLoader cl = getClass().getClassLoader();
        for (String mod : mod_compats) {
            if (!Loader.isModLoaded(mod)) {
                continue;
            }
            try {
                cl.loadClass(base_name + "Compat_" + mod).newInstance();
            } catch (Exception e) {
                // Err, strange
                Core.logWarning("Failed to load compatability module for " + mod);
                e.printStackTrace();
            }
        }
    }
}
