package factorization.coremod;

import net.minecraftforge.fml.common.asm.transformers.AccessTransformer;

import java.io.IOException;

public class FzAccessTransformer extends AccessTransformer {
    
    public FzAccessTransformer() throws IOException {
        super("factorization_at.cfg");
    }

}
