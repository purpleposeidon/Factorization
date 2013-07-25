package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.servo.ActuatorItem;
import factorization.common.servo.Instruction;
import factorization.common.servo.ServoMotor;

public class ActivateActuator extends Instruction {

    @Override
    public Icon getIcon(ForgeDirection side) {
        if (sneaky) {
            return BlockIcons.servo$activate_sneaky;
        }
        return BlockIcons.servo$activate;
    }
    
    boolean sneaky = false;

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.worldObj.isRemote) {
            return;
        }
        //NORELEASE this code should be in ServoMotor
        ItemStack is = motor.getHeldItem();
        if (is == null) {
            return;
        }
        try {
            EntityPlayer player = motor.getPlayer();
            player.setSneaking(sneaky);
            MovingObjectPosition mop = player.rayTrace(1.0, 1); //NORELEASE client-side only!
            if (mop == null) {
                mop = rayTrace(motor);
            }
            Core.notify(null, new Coord(motor.worldObj, mop), "X"); //NORELEASE
            if (is.getItem() instanceof ActuatorItem) {
                ActuatorItem ai = (ActuatorItem) is.getItem();
                ai.use(is, player, motor, mop);
            } else if (mop.typeOfHit == EnumMovingObjectType.TILE) {
                Vec3 hitVec = mop.hitVec;
                int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
                float dx = (float) (hitVec.xCoord - x);
                float dy = (float) (hitVec.yCoord - y);
                float dz = (float) (hitVec.zCoord - z);
                is.tryPlaceItemIntoWorld(player, motor.worldObj, x, y, z, mop.sideHit, dx, dy, dz);
            } else if (mop.typeOfHit == EnumMovingObjectType.ENTITY) {
                if (mop.entityHit.func_130002_c(player)) {
                    return;
                }
                if (mop.entityHit instanceof EntityLiving) {
                    is.func_111282_a(player, (EntityLiving)mop.entityHit);
                }
            }
        } catch (IOException e) { } finally {
            if (is.stackSize <= 0) {
                motor.getInv().set(0, null);
            }
            motor.finishUsingPlayer();
        }
    }
    
    MovingObjectPosition rayTrace(ServoMotor motor) {
        //First look for entities.
        Coord c = motor.getCurrentPos();
        ForgeDirection fd = motor.orientation.top;
        AxisAlignedBB ab = AxisAlignedBB.getAABBPool().getAABB(
                c.x + fd.offsetX, c.y + fd.offsetY, c.z + fd.offsetZ,  
                c.x + 1 + fd.offsetX, c.y + 1 + fd.offsetY, c.z + 1 + fd.offsetZ);
        for (Entity entity : (Iterable<Entity>)motor.worldObj.getEntitiesWithinAABBExcludingEntity(motor, ab)) {
            if (!entity.canBeCollidedWith()) {
                continue;
            }
            return new MovingObjectPosition(entity); //TODO: This isn't the right way to do this.
        }
        Coord targetBlock = c.add(fd);
        Vec3 nullVec = Vec3.createVectorHelper(fd.offsetX, fd.offsetY, fd.offsetZ);
        if (targetBlock.getCollisionBoundingBoxFromPool() != null) {
            return targetBlock.createMop(fd.getOpposite(), nullVec); //Something in the way
        }
        Coord surfaceBlock = targetBlock.add(fd);
        if (surfaceBlock.getCollisionBoundingBoxFromPool() != null) {
            return surfaceBlock.createMop(fd.getOpposite(), nullVec); //Something to click against
        }
        if (!c.isAir()) {
            nullVec.xCoord *= -1;
            nullVec.yCoord *= -1;
            nullVec.zCoord *= -1;
            return c.createMop(fd, nullVec);
        }
        return null;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        if (playerHasProgrammer(player)) {
            sneaky = !sneaky;
            return true;
        }
        return false;
    }
    
    @Override
    public String getInfo() {
        if (sneaky) {
            return "Shift Click";
        }
        return "Normal Click";
    }

    @Override
    public String getName() {
        return "fz.instruction.activateactuator";
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        sneaky = data.asSameShare(prefix + "sneak").putBoolean(sneaky);
        return this;
    }
    
    @Override
    protected ItemStack getRecipeItem() {
        return new ItemStack(Item.redstone);
    }
    
    @Override
    public boolean interrupts() {
        return true;
    }
}
