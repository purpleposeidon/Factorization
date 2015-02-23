package factorization.coremodhooks;

import cpw.mods.fml.common.eventhandler.EventBus;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import factorization.common.Command;
import factorization.common.FactorizationKeyHandler;
import factorization.common.FzConfig;
import factorization.docs.DocumentationModule;
import net.minecraftforge.event.world.WorldEvent;

public class HookTargetsClient {
    public static void keyTyped(char chr, int keysym) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;
        EntityPlayer player = mc.thePlayer;
        if (FzConfig.pocket_craft_anywhere) {
            if (FactorizationKeyHandler.pocket_key.getKeyCode() == keysym) {
                Command.craftOpen.call(player);
            }
        }
        if (chr == '?') {
            DocumentationModule.openPageForHilightedItem();
        }
    }
    
    public static boolean attackButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleAttackKeyEvent());
    }
    
    public static boolean useButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleUseKeyEvent());
    }
    
    private static boolean hasColliders(World world, Vec3 traceStart) {
        Chunk c = world.getChunkFromBlockCoords((int) traceStart.xCoord, (int) traceStart.zCoord);
        if (c == null) return false;
        IExtraChunkData cd = (IExtraChunkData) c;
        Entity[] colliders = cd.getConstantColliders();
        if (colliders == null || colliders.length == 0) return false;
        return true;
    }
    
    public static MovingObjectPosition boxTrace(World world, Vec3 traceStart, Vec3 traceEnd) {
        MovingObjectPosition ret = world.rayTraceBlocks(SpaceUtil.copy(traceStart), SpaceUtil.copy(traceEnd));
        if (!hasColliders(world, traceStart) && !hasColliders(world, traceEnd)) return ret;
        
        Entity box = new Entity(world) {
            @Override protected void entityInit() { }
            @Override protected void readEntityFromNBT(NBTTagCompound tag) { }
            @Override protected void writeEntityToNBT(NBTTagCompound tag) { }
        };
        double d = 0.2; //4.0/16.0;
        box.setPosition(traceStart.xCoord, traceStart.yCoord, traceStart.zCoord);
        box.boundingBox.minX = traceStart.xCoord - d;
        box.boundingBox.minY = traceStart.yCoord - d;
        box.boundingBox.minZ = traceStart.zCoord - d;
        box.boundingBox.maxX = traceStart.xCoord + d;
        box.boundingBox.maxY = traceStart.yCoord + d;
        box.boundingBox.maxZ = traceStart.zCoord + d;
        
        double dx = traceEnd.xCoord - traceStart.xCoord;
        double dy = traceEnd.yCoord - traceStart.yCoord;
        double dz = traceEnd.zCoord - traceStart.zCoord;
        
        int iterations = 8;
        
        double meh = 1.0 / (iterations + 1);
        for (int i = 0; i < iterations; i++) {
            box.moveEntity(dx * meh, dy * meh, dz * meh);
        }
        
        Vec3 hit = Vec3.createVectorHelper(box.posX, box.posY, box.posZ);
        
        {
            box.boundingBox.minX = box.posX - d;
            box.boundingBox.minY = box.posY - d;
            box.boundingBox.minZ = box.posZ - d;
            box.boundingBox.maxX = box.posX + d;
            box.boundingBox.maxY = box.posY + d;
            box.boundingBox.maxZ = box.posZ + d;
        }
        
        if (ret == null || ret.hitVec == null || ret.typeOfHit == MovingObjectType.MISS) return new MovingObjectPosition(null, hit);
        
        double retLen = ret.hitVec.lengthVector();
        
        dx = box.posX - traceStart.xCoord;
        dy = box.posY - traceStart.yCoord;
        dz = box.posZ - traceStart.zCoord;
        
        
        if (retLen * retLen > dx * dx + dy * dy + dz * dz) {
            return ret;
        }
        return new MovingObjectPosition(null, hit);
    }

    public static ThreadLocal<Boolean> abort = new ThreadLocal<Boolean>();
    public static boolean abortClientLoadEvent(EventBus bus, WorldEvent.Load event) {
        if (abort.get() == Boolean.TRUE) return false;
        return bus.post(event);
    }
}
