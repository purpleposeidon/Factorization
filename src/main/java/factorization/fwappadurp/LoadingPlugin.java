package factorization.fwappadurp;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class LoadingPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) { }

    @Override
    public String getModContainerClass() {
        return null; //NORELEASE
    }
    
    @Override
    public String getAccessTransformerClass() {
        return "factorization.fwappadurp.FwappaDurp";
    }

}
