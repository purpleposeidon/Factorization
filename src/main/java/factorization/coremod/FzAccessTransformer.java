package factorization.coremod;

import java.io.IOException;

import cpw.mods.fml.common.asm.transformers.AccessTransformer;

public class FzAccessTransformer extends AccessTransformer {
    
    public FzAccessTransformer() throws IOException {
        super("factorization_at.cfg");
    }

}
