package factorization.coremod;

import java.util.List;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import factorization.coremodhooks.HookTargetsClient;
import factorization.coremodhooks.HookTargetsServer;

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
    
    // Chunk.getEntitiesWithinAABBForEntity
    public void func_76588_a(Entity p_76588_1_, AxisAlignedBB p_76588_2_, List p_76588_3_, IEntitySelector p_76588_4_) {
        HookTargetsServer.addConstantColliders(this, p_76588_1_, p_76588_2_, p_76588_3_, p_76588_4_);
    }
    
    // EntityRenderer.orientCamera; method replacement
    public static MovingObjectPosition func_78467_g(WorldClient world, Vec3 traceStart, Vec3 traceEnd) {
        return HookTargetsClient.boxTrace(world, traceStart, traceEnd);
    }

    // World.checkBlockCollision
    public boolean func_72829_c(AxisAlignedBB box) {
        return HookTargetsServer.checkHammerCollision((World) (Object) this, box);
    }

    // Explosion.doExplosionA; method replacement
    public static double func_77278_a(Entity ent, double dmg) {
        return HookTargetsServer.clipExplosionResistance(ent, dmg);
    }
}
