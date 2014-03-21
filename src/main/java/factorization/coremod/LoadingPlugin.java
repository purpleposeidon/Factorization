package factorization.coremod;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class LoadingPlugin implements IFMLLoadingPlugin {
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }
    @Override public String getModContainerClass() { return null; } // We use the FMLCorePluginContainsFMLMod manifest attribute
    
    @Override
    public String getAccessTransformerClass() {
        return "factorization.coremod.FzAccessTransformer";
    }

    @Override public String[] getASMTransformerClass() {
        return new String[] { "factorization.coremod.ASMTransformer" };
    }
}
