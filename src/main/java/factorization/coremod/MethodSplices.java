package factorization.coremod;

import factorization.coremodhooks.HookTargetsClient;
import factorization.coremodhooks.HookTargetsServer;
import net.minecraft.init.Blocks;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

public class MethodSplices {
    // Block.onBlockDestroyedByExplosion
    public void func_149723_a(World world, int x, int y, int z, Explosion explosion) {
        HookTargetsServer.diamondExploded(this, world, x, y, z);
    }
    
    // Block.canDropFromExplosion
    public boolean func_149659_a(Explosion explosion) {
        if ((Object) this == Blocks.diamond_block) {
            return false;
        }
        return true; // This is sliiightly unfortunate.
        // If another coremod is messing with this function, there could be trouble.
        // Overriders won't be an issue tho.
    }
    
    // GuiContainer.keyTyped
    public void func_73869_a(char chr, int keysym) {
        HookTargetsClient.keyTyped(chr, keysym);
    }
    
    // Minecraft.func_147116_af "attack key pressed" function (first handler), MCPBot name clickMouse
    public void func_147116_af() {
        if (HookTargetsClient.attackButtonPressed()) {
            return;
        }
        return;
    }
    
    // Minecraft.func_147121_ag "use key pressed" function, MCPBot name rightClickMouse
    public void func_147121_ag() {
        if (HookTargetsClient.useButtonPressed()) {
            return;
        }
        return;
    }
}
