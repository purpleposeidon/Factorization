package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.servo.Decorator;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class WoodenServoGrate extends Decorator {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public void motorHit(ServoMotor motor) { }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return Block.trapdoor.getBlockTextureFromSide(0);
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        return false; //I suppose we could have something to turn it on and off. Erm.
    }

    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }

    @Override
    public String getName() {
        return "fz.decorator.servoGrateWood";
    }
    
    @Override
    public float getSize() {
        return 0;
    }

    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                " # ",
                "#-#",
                " # ",
                '-', Core.registry.servorail_item,
                '#', Block.trapdoor);
    }
}
