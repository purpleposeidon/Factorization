package factorization.coremod;

import net.minecraft.init.Blocks;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class MethodAppends {
    public void func_149723_a(World world, int x, int y, int z, Explosion explosion) {
        HookTargets.diamondExploded(this, world, x, y, z);
    }
    
    public void func_73869_a(char chr, int keysym) {
        HookTargets.keyTyped(chr, keysym);
    }
    
    public boolean func_149659_a(Explosion p_149659_1_) {
        if ((Object) this == Blocks.diamond_block) {
            return false;
        }
        return true; // This is sliiightly unfortunate.
        // If another coremod is messing with this function, there could be trouble.
        // Overriders won't be an issue tho.
    }
}
