package factorization.common.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.Core;
import factorization.common.TileEntityCommon;
import factorization.common.servo.Decorator;
import factorization.common.servo.ServoMotor;

public class RedstonePulse extends Decorator {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        if (motor.worldObj.isRemote) {
            return;
        }
        TileEntityCommon tef = motor.getCurrentPos().getTE(TileEntityCommon.class);
        if (tef == null) {
            return; //Just back away, very slowly...
        }
        tef.pulse();
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$pulse;
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return false;
    }

    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }

    @Override
    public String getName() {
        return "fz.decorator.pulse";
    }

    @Override
    public float getSize() {
        return 0F/16F;
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                "-_-",
                '-', Core.registry.servorail_item,
                '_', Block.pressurePlateStone);
    }
}
