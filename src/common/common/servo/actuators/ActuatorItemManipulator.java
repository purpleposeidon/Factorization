package factorization.common.servo.actuators;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.servo.ActuatorItem;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ActuatorItemManipulator extends ActuatorItem {
    public ActuatorItemManipulator(int itemId) {
        super(itemId, "servo/actuator.item_manipulator");
    }
    
    private static class State implements IDataSerializable {
        int slot = -1, limit = 64;

        @Override
        public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
            slot = data.asSameShare(prefix + "slot").put(slot);
            limit = data.asSameShare(prefix + "limit").put(limit);
            return this;
        }		
    }

    boolean giveItem(State state, ItemStack actuator_is, Entity user, MovingObjectPosition mop) throws IOException {
        FzInv target_inv = getInv(user, mop);
        if (target_inv == null) {
            return false;
        }
        int start = state.slot, end = state.slot + 1;
        if (state.slot == -1) {
            start = 0;
            end = target_inv.size();
        }
        
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
                        buffer = inv.pull(i, state.limit);
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
            int orig_size = toUse.stackSize;
            
            for (int i = start; i < end; i++) {
                if (target_inv.get(i) == null) {
                    continue;
                }
                toUse = FactorizationUtil.normalize(target_inv.pushInto(i, toUse));
                if (toUse == null) {
                    ss.remove(toUse_index);
                    return true;
                }
            }
            if (toUse != null) {
                for (int i = start; i < end; i++) {
                    toUse = FactorizationUtil.normalize(target_inv.pushInto(i, toUse));
                    if (toUse == null) {
                        ss.remove(toUse_index);
                        return true;
                    }
                }
            }
            return orig_size != FactorizationUtil.getStackSize(toUse);
        }
        return false;
    }
    
    boolean takeItem(State state, ItemStack actuator_is, Entity user, MovingObjectPosition mop) throws IOException {
        FzInv inv = getInv(user, mop);
        if (inv == null) {
            return false;
        }
        int start = state.slot, end = state.slot + 1;
        if (state.slot == -1) {
            start = 0;
            end = inv.size();
        }
        
        for (int i = start; i < end; i++) {
            ItemStack is = inv.pull(i, state.limit);
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
    public boolean use(ItemStack is, Entity user, MovingObjectPosition mop) throws IOException {
        State state = (new DataInNBT(FactorizationUtil.getTag(is))).as(Share.VISIBLE, "").put(new State());
        if (isSneaking(user)) {
            giveItem(state, is, user, mop);
        } else {
            takeItem(state, is, user, mop);
        }
        if (user instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) user;
            Core.proxy.updatePlayerInventory(player);
        }
        return true;
    }
    
    @Override
    public IDataSerializable getState() {
        return new State();
    }
}
