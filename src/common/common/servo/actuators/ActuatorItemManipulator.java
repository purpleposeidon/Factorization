package factorization.common.servo.actuators;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.servo.ActuatorItem;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ActuatorItemManipulator extends ActuatorItem {
    public ActuatorItemManipulator(int itemId) {
        super(itemId);
        setUnlocalizedName("factorization:servo/actuator.item_manipulator");
    }
    
    private static int default_slot = -1, default_limit = 64;

    boolean giveItem(ItemStack actuator_is, Entity user, MovingObjectPosition mop) {
        FzInv target_inv = getInv(user, mop);
        if (target_inv == null) {
            return false;
        }
        int slot = takeConfig(user, default_slot);
        int start = slot, end = slot;
        if (slot == -1) {
            start = 0;
            end = target_inv.size();
        }
        int limit = takeConfig(user, default_limit);
        
        //TODO: Implement slot accesses...
        ItemStack toPush = null;
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            FzInv inv = FactorizationUtil.openInventory(player.inventory, ForgeDirection.UP);
            ItemStack toUse = inv.peek();
            ItemStack buffer = null;
            boolean any = false;
            for (int i = start; i < end; i++) {
                ItemStack peak = FactorizationUtil.normalize(inv.get(i));
                if (peak == actuator_is || peak == null) {
                    continue;
                }
                if (target_inv.canInsert(i, toUse)) {
                    if (buffer == null) {
                        buffer = inv.pull(i, limit);
                    }
                    buffer = target_inv.push(buffer);
                    if (buffer == null) {
                        return true;
                    }
                    any |= true;
                }
            }
            return any;
        } else if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            ServoStack ss = motor.getServoStack(ServoMotor.STACK_ARGUMENT);
            
            ItemStack toUse = null;
            int toUse_index = 0;
            for (Object o : ss) {
                if (o instanceof ItemStack && o != actuator_is) {
                    toUse = (ItemStack) o;
                    break;
                }
                toUse_index++;
            }
            if (toUse == null) {
                return false;
            }
            ItemStack buffer = null;
            boolean any = false;
            for (int i = start; i < end; i++) {
                if (target_inv.canInsert(i, toUse)) {
                    if (buffer == null) {
                        buffer = (ItemStack) ss.remove(toUse_index);
                    }
                    buffer = target_inv.pushInto(i, buffer);
                    if (buffer == null) {
                        return true;
                    }
                    any |= true;
                }
            }
            return any;
        }
        return false;
    }
    
    boolean takeItem(ItemStack actuator_is, Entity user, MovingObjectPosition mop) {
        FzInv inv = getInv(user, mop);
        if (inv == null) {
            return false;
        }
        int slot = takeConfig(user, default_slot);
        int start = slot, end = slot;
        if (slot == -1) {
            start = 0;
            end = inv.size();
        }
        int limit = takeConfig(user, default_limit);
        
        for (int i = start; i < end; i++) {
            ItemStack is = inv.pull(i, limit);
            if (is == null) {
                continue;
            }
            pushItem(user, is);
            return true;
        }
        return false;
    }
    
    FzInv getInv(Entity user, MovingObjectPosition mop) {
        if (mop.typeOfHit == EnumMovingObjectType.TILE) {
            Coord here = new Coord(user.worldObj, mop);
            return FactorizationUtil.openInventory(here.getTE(IInventory.class), ForgeDirection.getOrientation(mop.sideHit));
        } else if (mop.typeOfHit == EnumMovingObjectType.ENTITY) {
            return FactorizationUtil.openInventory(mop.entityHit, false);
        } else {
            return null;
        }
    }
    @Override
    public boolean use(ItemStack is, Entity user, MovingObjectPosition mop) {
        if (isSneaking(user)) {
            giveItem(is, user, mop);
        } else {
            takeItem(is, user, mop);
        }
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            Core.proxy.updatePlayerInventory(player);
        }
        return true;
    }

}
