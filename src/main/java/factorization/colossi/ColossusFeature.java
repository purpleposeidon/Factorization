package factorization.colossi;

import factorization.fzds.DeltaChunk;
import factorization.fzds.HammerInfo;
import factorization.shared.Core;

public class ColossusFeature {
    static int deltachunk_channel = 88;
    
    public static void init() {
        MaskLoader.loadMasks();
        HammerInfo reg = DeltaChunk.getHammerRegistry();
        reg.makeChannelFor(Core.name, "collossi", deltachunk_channel, 24, "Channel for awakened colossus body parts");
    }
}
