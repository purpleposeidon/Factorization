package factorization.coremod;

import java.util.ArrayList;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.DependsOn;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.SortingIndex;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

@SortingIndex(1)
@MCVersion("1.7.10") // NORELEASE: Check that this matches our MC version! (Or just automate it...)
@TransformerExclusions("factorization.coremod.")
@DependsOn("cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer")
public class LoadingPlugin implements IFMLLoadingPlugin {
    public static boolean deobfuscatedEnvironment = true;
    private boolean inspect_air = false;
    @Override public String getSetupClass() { return null; }
    @Override public String getModContainerClass() { return null; } // We use the FMLCorePluginContainsFMLMod manifest attribute
    
    @Override
    public String getAccessTransformerClass() {
        return "factorization.coremod.FzAccessTransformer";
    }

    @Override
    public String[] getASMTransformerClass() {
        ArrayList<String> plugins = new ArrayList();
        plugins.add("factorization.coremod.ASMTransformer");
        if (inspect_air) {
            plugins.add("factorization.coremod.AirInspector");
        }
        String[] ret = new String[plugins.size()];
        plugins.toArray(ret);
        return ret;
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
        deobfuscatedEnvironment = !(Boolean) data.get("runtimeDeobfuscationEnabled");
        inspect_air = "true".equalsIgnoreCase(System.getProperty("factorization.inspectAir"));
    }
}
