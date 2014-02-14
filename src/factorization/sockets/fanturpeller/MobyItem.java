package factorization.sockets.fanturpeller;

import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.shared.FzUtil;
import factorization.shared.Sound;

public class MobyItem extends Item {

    public MobyItem(int itemId) {
        super(itemId);
    }
    
    static class GhastState {
        static ThreadLocal<GhastState> state = new ThreadLocal<MobyItem.GhastState>();
        
        float dx, dy;
        int waypointX, waypointY;
        
        Random rand = new Random();
        
        private void load(NBTTagCompound tag) {
            dx = tag.getFloat("dx");
            dy = tag.getFloat("dy");
            if (!tag.hasKey("wpX")) {
                waypointX = rand.nextInt(9);
                waypointY = rand.nextInt(4);
            } else {
                waypointX = tag.getInteger("wpX");
                waypointY = tag.getInteger("wpY");
            }
        }
        static GhastState read(NBTTagCompound tag) {
            GhastState gs = state.get();
            if (gs == null) {
                state.set(new GhastState());
            }
            gs.read(tag);
            return gs;
        }
        
        void write(NBTTagCompound tag) {
            // :|
            tag.setFloat("dx", dx);
            tag.setFloat("dy", dy);
            tag.setInteger("wpX", waypointX);
            tag.setInteger("wpY", waypointY);
        }
        
        void tick(EntityPlayer player, NBTTagCompound tag, int slotIndex) {
            int intX = slotIndex % 9;
            int intY;
            if (slotIndex < 9) {
                intY = 3;
            } else {
                intY = (slotIndex - 9)/9;
            }
            boolean needWaypoint = false;
            if (intX == waypointX && intY == waypointY) {
                needWaypoint = true;
            }
            float x = intX + dx;
            float y = intY + dy;
            Vec3 vec = player.worldObj.getWorldVec3Pool().getVecFromPool(waypointX - x, waypointY - y, 0);
            vec = vec.normalize();
            float speed = 0.02F;
            x += vec.xCoord*speed;
            y += vec.yCoord*speed;
            
            int newX = (int) x;
            int newY = (int) y;
            int newSlot = newX % 9;
            if (newY != 3) {
                newSlot += newY*9;
            }
            
            if (needWaypoint || newSlot != slotIndex && player.inventory.getStackInSlot(newSlot) != null) {
                waypointX = rand.nextInt(9);
                waypointY = rand.nextInt(4);
                return;
            }
            dx = x - newX;
            dy = y - newY;
        }
    }
    
    int last_ghast_noise = 20*10;
    
    @Override
    public void onUpdate(ItemStack is, World world, Entity ent, int slotIndex, boolean isHeld) {
        if (world == null || ent == null) return;
        if (is.getItemDamage() != 0) return;
        if (!(ent instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) ent;
        if (world.isRemote) {
            last_ghast_noise--;
            if (last_ghast_noise > 0) return;
            last_ghast_noise = (int) (20*(4 + 30*Math.random()));
            if (isHeld) {
                //Sound.ghastletHeld.play();
            } else {
                //Sound.ghastletWander.play();
            }
        }
        if (!isHeld) {
            NBTTagCompound tag = FzUtil.getTag(is);
            GhastState gs = GhastState.read(tag);
            gs.tick(player, tag, slotIndex);
            gs.write(tag);
        }
    }
    
}
