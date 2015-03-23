package factorization.mechanisms;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.fzds.DeltaChunk;
import factorization.shared.Core;

public class MechanismsFeature {
    static int deltachunk_channel;

    public static void initialize() {
        if (!DeltaChunk.enabled()) return;
        deltachunk_channel = DeltaChunk.getHammerRegistry().makeChannelFor(Core.modId, "mechanisms", 11, 64, "Hinges & cranks");
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            MechanismClientFeature.initialize();
        }
    }

}
