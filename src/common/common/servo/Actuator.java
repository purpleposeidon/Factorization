package factorization.common.servo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.FactorizationUtil;

public abstract class Actuator extends ServoComponent {
    protected abstract boolean use(Entity user, MovingObjectPosition mop);
    
    public void onIdle(Entity user) { }
    
    
    
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
    
    TraceHelper trace_helper = new TraceHelper();
    
    final public boolean onUse(Entity user, boolean sneaking) {
        MovingObjectPosition target = null;
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            target = player.rayTrace(4.5, 1);
        } else if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            trace_helper.posX = user.posX;
            trace_helper.posY = user.posY;
            trace_helper.posZ = user.posZ;
            trace_helper.worldObj = user.worldObj;
            trace_helper.look = motor.orientation.facing;
            target = trace_helper.rayTrace(4.5, 1);
        }
        if (target == null) {
            return false;
        }
        return use(user, target);
    }
    
    @Override
    final public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return onUse(player, player.isSneaking());
    }
    
    @Override
    final public boolean onClick(EntityPlayer player, ServoMotor motor) {
        if (motor.getActuator() != null) {
            return false;
        }
        motor.setActuator(this);
        return true;
    }
    
    public static ItemStack takeItem(Entity user) {
        if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            return motor.getServoStack().popType(ItemStack.class);
        } else if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            return FactorizationUtil.openInventory(player.inventory, ForgeDirection.UP).pull();
        }
        return null;
    }
    
    public static ItemStack pushItem(Entity user, ItemStack is) {
        if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            motor.getServoStack().pushmergeItemStack(is);
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
}
