package factorization.common.servo;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.Core;
import factorization.common.Core.TabType;
import factorization.common.ItemCraftingComponent;

public abstract class ActuatorItem extends ItemCraftingComponent {
    public ActuatorItem(int itemId, String name) {
        super(itemId, name);
        Core.tab(this, TabType.SERVOS);
    }

    public abstract boolean use(ItemStack is, Entity user, MovingObjectPosition mop) throws IOException;
    
    static class TraceHelper extends EntityLiving {
        public TraceHelper() {
            super(null);
        }

        public ForgeDirection look = ForgeDirection.UP;
        @Override
        public int getMaxHealth() { return 0; }
        
        public Vec3 getLook() {
            return Vec3.createVectorHelper(look.offsetX, look.offsetY, look.offsetZ);
        }
    }
    
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        MovingObjectPosition mop = new MovingObjectPosition(x, y, z, side, Vec3.createVectorHelper(hitX, hitY, hitZ));
        try {
            use(stack, player, mop);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    
    public boolean onUse(ItemStack is, Entity user) {
        MovingObjectPosition target = null;
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            target = player.rayTrace(4.5, 1);
        } else if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            Coord c = motor.getCurrentPos();
            ForgeDirection fd = motor.orientation.top;
            target = new MovingObjectPosition(c.x + fd.offsetX, c.y + fd.offsetY, c.z + fd.offsetZ, fd.getOpposite().ordinal(), Vec3.createVectorHelper(0, 0, 0));
        }
        if (target == null) {
            return false;
        }
        try {
            return use(is, user, target);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean extractEnergy(Entity user, int amount) {
        if (amount <= 0) {
            return true;
        }
        if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
        } else if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
        }
        return true; //TODO: implement Actuator.extractEnergy
    }
    
    public static boolean isSneaking(Entity user) {
        if (user instanceof EntityLiving) {
            return ((EntityLiving) user).isSneaking();
        } else if (user instanceof ServoMotor) {
            return ((ServoMotor) user).sneaking;
        } else {
            return false;
        }
    }
    
    //final static public String actcfg = "actcfg";
    
    /*
    public final DataHelper getConfigReader(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        return new DataInNBT(tag).as(Share.VISIBLE, "");
    }
    
    public final DataHelper getConfigWriter(ItemStack is) {
        NBTTagCompound tag = FactorizationUtil.getTag(is);
        return new DataOutNBT(tag).as(Share.VISIBLE, "");
    }*/
    
    public abstract IDataSerializable getState();
}
