package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.servo.Decorator;
import factorization.servo.ServoMotor;
import factorization.servo.TileEntityServoRail;
import factorization.shared.Core;

public class ScanColor extends Decorator {
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Coord at = motor.getCurrentPos();
        at = at.add(motor.orientation.top);
        motor.getServoStack(ServoMotor.STACK_ARGUMENT).push(FzColor.readColor(at));
    }

    @Override
    public Icon getIcon(ForgeDirection side) {
        return BlockIcons.servo$scan_color;
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
        return "fz.decorator.scancolor";
    }
    
    @Override
    protected void addRecipes() {
        Core.registry.recipe(toItem(),
                "+Q+",
                "Q#Q",
                "+Q+",
                '+', FactoryType.SERVORAIL.itemStack(),
                'Q', Item.netherQuartz,
                '#', Core.registry.logicMatrixIdentifier);
    }
    
    @Override
    public float getSize() {
        return super.getSize() - 1F/32F;
    }
    
    @Override
    public boolean collides() {
        return false;
    }
}
