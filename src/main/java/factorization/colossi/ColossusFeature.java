package factorization.colossi;

import factorization.fzds.DeltaChunk;
import factorization.fzds.HammerInfo;

public class ColossusFeature {
    static int deltachunk_channel = 88;
    
    public static void init() {
        MaskLoader.loadMasks();
        HammerInfo reg = DeltaChunk.getHammerRegistry();
        reg.makeChannelFor("FactorizationColossi", "collossi", deltachunk_channel, 24, "Channel for awakened colossi body parts");
    }
}
