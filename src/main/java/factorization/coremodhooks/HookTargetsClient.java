package factorization.coremodhooks;

import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;

public class HookTargetsClient {
    public static void keyTyped(char chr, int keysym) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        MinecraftForge.EVENT_BUS.post(new UnhandledGuiKeyEvent(chr, keysym, mc.thePlayer, mc.currentScreen));
    }
    
    public static boolean attackButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleAttackKeyEvent());
    }
    
    public static boolean useButtonPressed() {
        return MinecraftForge.EVENT_BUS.post(new HandleUseKeyEvent());
    }
    
    private static boolean hasColliders(World world, Vec3 traceStart) {
        Chunk c = world.getChunkFromChunkCoords((int) (traceStart.xCoord) >> 4, (int) (traceStart.zCoord) >> 4);
        if (c == null) return false;
        IExtraChunkData cd = (IExtraChunkData) c;
        Entity[] colliders = cd.getConstantColliders();
        if (colliders == null || colliders.length == 0) return false;
        return true;
    }
    
    public static MovingObjectPosition boxTrace(World world, Vec3 traceStart, Vec3 traceEnd) {
        MovingObjectPosition ret = world.rayTraceBlocks(SpaceUtil.copy(traceStart), SpaceUtil.copy(traceEnd));
        if (!hasColliders(world, traceStart) && !hasColliders(world, traceEnd)) return ret;

        final double d = 0.2; //4.0/16.0;
        Entity box = new Entity(world) {
            @Override protected void entityInit() { }
            @Override protected void readEntityFromNBT(NBTTagCompound tag) { }
            @Override protected void writeEntityToNBT(NBTTagCompound tag) { }

            @Override
            public void setPosition(double x, double y, double z) {
                this.posX = x;
                this.posY = y;
                this.posZ = z;
                this.setEntityBoundingBox(new AxisAlignedBB(x - d, y, z - d, x + d, y + d, z + d));
            }
        };
        box.setPosition(traceStart.xCoord, traceStart.yCoord, traceStart.zCoord);

        double dx = traceEnd.xCoord - traceStart.xCoord;
        double dy = traceEnd.yCoord - traceStart.yCoord;
        double dz = traceEnd.zCoord - traceStart.zCoord;
        
        int iterations = 8;
        
        double meh = 1.0 / (iterations + 1);
        for (int i = 0; i < iterations; i++) {
            box.moveEntity(dx * meh, dy * meh, dz * meh);
        }
        
        Vec3 hit = new Vec3(box.posX, box.posY, box.posZ);
        box.setPosition(box.posX, box.posY, box.posZ);
        
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

    public static ThreadLocal<Boolean> clientWorldLoadEventAbort = new ThreadLocal<Boolean>();
    public static boolean abortClientLoadEvent(EventBus bus, WorldEvent.Load event) {
        if (clientWorldLoadEventAbort.get() == Boolean.TRUE) return false;
        return bus.post(event);
    }
}
