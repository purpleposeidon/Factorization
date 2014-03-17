package factorization.fwappadurp;

import java.io.IOException;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

public class FwappaDurp extends AccessTransformer {
    
    public FwappaDurp() throws IOException {
        // Fwappa Durp! He's the bAT!
        super("factorization_at.cfg");
    }

}
