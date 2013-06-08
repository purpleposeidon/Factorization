package factorization.common.servo.actuators;

import java.io.IOException;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.servo.Actuator;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class ItemManipulator extends Actuator {
    ForgeDirection sneak_direction = ForgeDirection.UNKNOWN;
    int slot = -1, quantity = -1;

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        sneak_direction = data.as(Share.VISIBLE, "sneak_direction").putEnum(sneak_direction);
        slot = data.as(Share.PRIVATE, "slot").putInt(slot);
        quantity = data.as(Share.PRIVATE, "quantity").putInt(quantity);
        return this;
    }

    @Override
    public String getName() {
        return "fz.actuator.item_manipulate";
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        // TODO Auto-generated method stub

    }
    
    @Override
    protected boolean use(Entity user, MovingObjectPosition mop) {
        if (isSneaking(user)) {
            return giveItem(user, mop);
        } else {
            return takeItem(user, mop);
        }
    }

    boolean giveItem(Entity user, MovingObjectPosition mop) {
        FzInv target_inv = getInv(user, mop);
        if (target_inv == null) {
            return false;
        }
        int start = slot, end = slot;
        if (slot == -1) {
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
                if (target_inv.canInsert(i, toUse)) {
                    if (buffer == null) {
                        buffer = inv.pull();
                    }
                    buffer = target_inv.pushInto(i, buffer);
                    if (buffer == null) {
                        return true;
                    }
                    any |= true;
                }
            }
            return any;
        } else if (user instanceof ServoMotor) {
            ServoMotor motor = (ServoMotor) user;
            ServoStack ss = motor.getServoStack();
            
            ItemStack toUse = ss.findType(ItemStack.class);
            ItemStack buffer = null;
            boolean any = false;
            for (int i = start; i < end; i++) {
                if (target_inv.canInsert(i, toUse)) {
                    if (buffer == null) {
                        buffer = ss.popType(ItemStack.class);
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
    
    boolean takeItem(Entity user, MovingObjectPosition mop) {
        FzInv inv = getInv(user, mop);
        if (inv == null) {
            return false;
        }
        int start = slot, end = slot;
        if (slot == -1) {
            start = 0;
            end = inv.size();
        }
        
        for (int i = start; i < end; i++) {
            ItemStack is = inv.pullFromSlot(i);
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

}
