package factorization.common.servo.actuators;

import java.io.IOException;
import java.util.List;

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

public class ActuatorItemSyringe extends ActuatorItem {
    public ActuatorItemSyringe(int itemId) {
        super(itemId, "servo/actuator.item_syringe");
        setFull3D();
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
        if (state.slot != -1) {
            target_inv = target_inv.slice(state.slot, state.slot + 1);
        }
        return FactorizationUtil.openInventory(user, true).transfer(target_inv, state.limit, actuator_is);
    }
    
    boolean takeItem(State state, ItemStack actuator_is, Entity user, MovingObjectPosition mop) throws IOException {
        FzInv target_inv = getInv(user, mop);
        if (target_inv == null) {
            return false;
        }
        if (state.slot != -1) {
            target_inv = target_inv.slice(state.slot, state.slot + 1);
        }
        FzInv user_inv = FactorizationUtil.openInventory(user, true);
        return target_inv.transfer(user_inv, state.limit, actuator_is);
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
        if (user.worldObj.isRemote) {
            return true;
        }
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
    
    @Override
    public void addConfigurationInfo(ItemStack is, List infoList) throws IOException {
        State state = (new DataInNBT(FactorizationUtil.getTag(is))).as(Share.VISIBLE, "").put(new State());
        infoList.add("Slot: " + (state.slot == -1 ? "Any" : state.slot));
        infoList.add("Size: " + state.limit);
    }
}
