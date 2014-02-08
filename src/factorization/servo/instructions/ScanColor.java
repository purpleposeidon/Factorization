package factorization.servo.instructions;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockStem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.IPlantable;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.servo.Decorator;
import factorization.servo.ServoMotor;
import factorization.shared.Core;

public class ScanColor extends Decorator {
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public void motorHit(ServoMotor motor) {
        Coord at = motor.getCurrentPos();
        at = at.add(motor.getOrientation().top);
        FzColor col = FzColor.readColor(at);
        if (col != null) {
            motor.getArgStack().push(col);
            return;
        }
        final FzColor[] colorArray = new FzColor[] {
                FzColor.LIME, FzColor.LIME, FzColor.LIME,
                FzColor.GREEN, FzColor.GREEN, FzColor.GREEN, FzColor.GREEN, 
                FzColor.YELLOW, FzColor.YELLOW, FzColor.YELLOW, FzColor.YELLOW, 
                FzColor.ORANGE, FzColor.ORANGE, FzColor.ORANGE, FzColor.ORANGE, 
                FzColor.RED
        }; //NORELEASE: static
        Block block = at.getBlock();
        if (block instanceof BlockReed || block instanceof BlockCactus) return; // Colors don't change, so give nothing.
        if (block instanceof IPlantable) {
            int md = ((IPlantable) block).getPlantMetadata(at.w, at.x, at.y, at.z);
            motor.getArgStack().push(colorArray[md]);
            return;
        }
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
