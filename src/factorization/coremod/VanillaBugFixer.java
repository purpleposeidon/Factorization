package factorization.coremod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.server.ServerListenThread;
import net.minecraft.server.ThreadMinecraftServer;
import cpw.mods.fml.common.asm.transformers.SideTransformer;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@TransformerExclusions(VanillaBugFixer.packageName)
public class VanillaBugFixer implements IFMLLoadingPlugin {
    //NORELEASE: Okay, but what happens if it runs on a stand-alone server? It probably doesn't even HAVE the functions. ;_;
    //If this is the case, we'll just have to patch them back in (with extra code for each override vanilla adds)
    //-Dfml.coreMods.load=factorization.coremod.VanillaBugFixer
    static final String packageName = "factorization.coremod";

    @Override
    //@Deprecated
    public String[] getLibraryRequestClass() { return null; }
    
    static final String sideStripper = packageName + "." + "StripSideOnly";

    @Override
    public String[] getASMTransformerClass() {
        Thread thr = Thread.currentThread();
        if ((thr instanceof ThreadMinecraftServer) || (thr instanceof ServerListenThread)) {
            return new String[] {
                    sideStripper
            };
        }
        return new String[] {
                sideStripper
        }; //NORELEASE
        //return new String[] {};
    }

    @Override
    public String getModContainerClass() {
        return packageName + "." + "BugFixerContainer";
    }

    @Override
    public String getSetupClass() { return null; }

    @Override
    public void injectData(Map<String, Object> data) {
        //We need to strip out the SideOnly annotations before SideTransformer removes the methods. 
        try {
            LaunchClassLoader lcl = (LaunchClassLoader) getClass().getClassLoader();
            Field f = LaunchClassLoader.class.getDeclaredField("transformers");
            f.setAccessible(true);
            List<IClassTransformer> transformers = (List<IClassTransformer>) f.get(lcl);
            ArrayList<IClassTransformer> copy = new ArrayList(transformers.size());
            
            
            IClassTransformer sideStripper = null;
            for (IClassTransformer robot : transformers) {
                if (robot.getClass() == StripSideOnly.class) {
                    sideStripper = robot;
                    break;
                }
            }
            if (sideStripper == null) {
                return;
            }
            
            for (IClassTransformer robot : transformers) {
                if (robot == sideStripper) {
                    continue;
                }
                if (robot.getClass() == SideTransformer.class) {
                    copy.add(sideStripper);
                }
                copy.add(robot);
            }
            transformers.clear();
            transformers.addAll(copy);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
