package factorization.coremod;

import com.google.common.base.Predicate;
import factorization.coremodhooks.HookTargetsClient;
import factorization.coremodhooks.HookTargetsServer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;

import java.util.List;

public class MethodSplices {
    // Block.onBlockDestroyedByExplosion
    public void func_180652_a(World world, int x, int y, int z, Explosion explosion) {
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
        // NORELEASE: Not needed; forge is here to save the day.
        HookTargetsClient.keyTyped(chr, keysym);
    }
    
    // Minecraft.func_147116_af "attack key pressed" function (first handler), MCPBot name clickMouse
    public void func_147116_af() {
        // NORELEASE: Maybe forge?
        if (HookTargetsClient.attackButtonPressed()) {
            return;
        }
        return;
    }
    
    // Minecraft.func_147121_ag "use key pressed" function, MCPBot name rightClickMouse
    public void func_147121_ag() {
        // NORELEASE: Maybe forge?
        if (HookTargetsClient.useButtonPressed()) {
            return;
        }
        return;
    }
    
    // Chunk.getEntitiesWithinAABBForEntity
    public void func_177414_a(Entity entity, AxisAlignedBB box, List<Entity> listToFill, Predicate<? super Entity> filter) {
        HookTargetsServer.addConstantColliders(this, entity, box, listToFill, filter);
    }
    
    // EntityRenderer.orientCamera; method replacement
    public static MovingObjectPosition func_78467_g(World world, Vec3 traceStart, Vec3 traceEnd) {
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

    public static boolean net$minecraft$client$multiplayer$WorldClient$init(EventBus bus, WorldEvent.Load event) {
        return HookTargetsClient.abortClientLoadEvent(bus, event);
    }

    public static boolean bdb$init(EventBus bus, WorldEvent.Load event) {
        // This is me being lazy. Dealing with names is obnoxious. Hopefully we can get this gone when we port to 1.9?
        return HookTargetsClient.abortClientLoadEvent(bus, event);
    }
}
