package factorization.compat;

import factorization.shared.Core;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.util.ArrayList;
import java.util.Locale;

public class CompatModuleLoader extends CompatBase {
    ArrayList<CompatBase> modules = new ArrayList<CompatBase>();

    String[] mod_compats = new String[] {"IC2", "Railcraft", "erebus"};
    String base_name = getClass().getCanonicalName().replace(getClass().getSimpleName(), "");
    
    public void loadCompat() {
        ClassLoader cl = getClass().getClassLoader();
        for (String mod : mod_compats) {
            if (!Loader.isModLoaded(mod)) {
                Core.logInfo(mod  + " not loaded; not loading compatibility module");
                continue;
            }
            // TODO: Config options
            try {
                String name = base_name + mod.toLowerCase(Locale.ROOT) + ".Compat_" + mod;
                Class<? extends CompatBase> compatClass = (Class<? extends CompatBase>) cl.loadClass(name);
                modules.add(compatClass.newInstance());
            } catch (Throwable e) {
                // A mystery!
                Core.logWarning("Failed to load compatability module for " + mod);
                e.printStackTrace();
                continue;
            }
            Core.logInfo(mod + " compatibility module loaded");
        }
    }

    @Override
    public void preinit(FMLPreInitializationEvent event) {
        for (CompatBase mod : modules) {
            try {
                mod.preinit(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void init(FMLInitializationEvent event) {
        for (CompatBase mod : modules) {
            try {
                mod.init(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    @Override
    public void postinit(FMLPostInitializationEvent event) {
        for (CompatBase mod : modules) {
            try {
                mod.postinit(event);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
