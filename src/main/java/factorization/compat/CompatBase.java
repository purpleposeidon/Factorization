package factorization.compat;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

public abstract class CompatBase {
    public void preinit(FMLPreInitializationEvent event) { }
    public void init(FMLInitializationEvent event) { }
    public void postinit(FMLPostInitializationEvent event) { }

    public static NBTTagList list(String ...args) {
        NBTTagList ret = new NBTTagList();
        for (String a : args) ret.appendTag(new NBTTagString(a));
        return ret;
    }

}
