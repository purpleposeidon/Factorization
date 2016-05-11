package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.iterator.ServoMotor;
import factorization.servo.rail.Decorator;
import factorization.shared.Core;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class PowerStation extends Decorator {
    @Override
    public void motorHit(ServoMotor motor) {
        motor.waitForPower();
    }

    @Override
    protected IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        return false;
    }

    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }

    @Override
    public String getName() {
        return "fz.decorator.powerstation";
    }

    @Override
    protected void addRecipes() {
        Core.registry.oreRecipe(toItem(),
                "|@|",
                "+|+",
                "|@|",
                '|', Core.registry.wirePlacer,
                '@', Core.registry.insulated_coil,
                '+', Core.registry.servorail);
    }

    static IFlatModel model;
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return model;
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        model = regDecor(maker, "powerstation");
    }
}
