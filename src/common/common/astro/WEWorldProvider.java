package factorization.common.astro;

import net.minecraft.src.Chunk;
import net.minecraft.src.WorldProvider;

public class WEWorldProvider extends WorldProvider {
    public WEWorldProvider() {
        hasNoSky = true;
        dimensionId = -2; //NOTE: ???
        generateLightBrightnessTable();
    }
    @Override
    public String getDimensionName() {
        return "FZWEWP";
    }

    @Override
    public boolean canRespawnHere() {
        return false;
    }
    
    @Override
    public String getSaveFolder() {
        return "FZWEWP_-2";
    }
    
    @Override
    public String getWelcomeMessage() {
        return "Entering a FZWE. You should never see this message...";
    }
    
    @Override
    public String getDepartMessage() {
        return "Leaving a FZWE. You should never see this message...";
    }
}
