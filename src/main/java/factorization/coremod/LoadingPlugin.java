package factorization.coremod;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class LoadingPlugin implements IFMLLoadingPlugin {
    public static boolean deobfuscatedEnvironment = true;
    @Override public String getSetupClass() { return null; }
    @Override public String getModContainerClass() { return null; } // We use the FMLCorePluginContainsFMLMod manifest attribute
    
    @Override
    public String getAccessTransformerClass() {
        return "factorization.coremod.FzAccessTransformer";
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { "factorization.coremod.ASMTransformer" };
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
        deobfuscatedEnvironment = !(Boolean) data.get("runtimeDeobfuscationEnabled");
    }
}
