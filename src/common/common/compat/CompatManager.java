package factorization.common.compat;

import cpw.mods.fml.common.Loader;

public class CompatManager {
    String[] mod_compats = new String[] {"IC2"};
    
    String base_name = getClass().getCanonicalName().replace(getClass().getSimpleName(), "");
    
    public void loadCompat() {
        ClassLoader cl = getClass().getClassLoader();
        for (String mod : mod_compats) {
            if (Loader.isModLoaded(mod)) {
                try {
                    cl.loadClass(base_name + "Compat_" + mod).newInstance();
                } catch (Exception e) {
                    // Err, strange
                    e.printStackTrace();
                }
            }
        }
    }
}
