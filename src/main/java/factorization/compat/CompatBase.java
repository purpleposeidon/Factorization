package factorization.compat;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public abstract class CompatBase {
    public void preinit(FMLPreInitializationEvent event) { }
    public void init(FMLInitializationEvent event) { }
    public void postinit(FMLPostInitializationEvent event) { }
}
