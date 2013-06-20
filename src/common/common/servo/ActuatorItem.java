package factorization.common.servo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.Core.TabType;

public abstract class ActuatorItem extends Item {
    public ActuatorItem(int itemId) {
        super(itemId);
        Core.tab(this, TabType.SERVOS);
    }

    public abstract boolean use(ItemStack is, Entity user, MovingObjectPosition mop);
    
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
    
    static TraceHelper trace_helper = new TraceHelper();
    
    /*
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        boolean sneak = player.isSneaking();
        onUse(stack, player, sneak);
        return true;
    }*/
    /*
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        onUse(is, player, player.isSneaking());
        return is;
    }*/
    
    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) {
            return false;
        }
        //return onUse(stack, player);
        MovingObjectPosition mop = new MovingObjectPosition(x, y, z, side, Vec3.createVectorHelper(hitX, hitY, hitZ));
        use(stack, player, mop);
        return true;
        // TODO Auto-generated method stub
        //return super.onItemUseFirst(stack, player, world, x, y, z, side, hitX, hitY, hitZ);
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
            /*trace_helper.posX = user.posX;
            trace_helper.posY = user.posY;
            trace_helper.posZ = user.posZ;
            trace_helper.worldObj = user.worldObj;
            trace_helper.look = motor.orientation.facing;
            target = trace_helper.rayTrace(4.5, 1);*/
        }
        if (target == null) {
            return false;
        }
        return use(is, user, target);
    }
    
    public static ItemStack takeItem(Entity user) {
        if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            return motor.getServoStack(ServoMotor.STACK_ARGUMENT).popType(ItemStack.class);
        } else if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            return FactorizationUtil.openInventory(player.inventory, ForgeDirection.UP).pull();
        }
        return null;
    }
    
    public static ItemStack pushItem(Entity user, ItemStack is) {
        if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            motor.getServoStack(ServoMotor.STACK_ARGUMENT).pushmergeItemStack(is);
            return FactorizationUtil.normalize(is);
        } else if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            return FactorizationUtil.openInventory(player.inventory, ForgeDirection.UP).push(is);
        }
        return is;
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
    
    public static <T> T takeConfig(Entity user, T default_value) {
        if (!(user instanceof ServoMotor)) {
            return default_value;
        }
        ServoMotor motor = (ServoMotor) user;
        ServoStack ss = motor.getServoStack(ServoMotor.STACK_CONFIG);
        T ret = ss.popType((Class<? extends T>)default_value.getClass());
        if (ret == null) {
            return default_value;
        }
        if (ServoMotor.canClone(ret)) {
            ss.append(ret);
        }
        return ret;
    }
}
