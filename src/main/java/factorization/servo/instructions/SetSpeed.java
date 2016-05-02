package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.CpuBlocking;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class SetSpeed extends Instruction {
    byte speed = 3;
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        if (data.isReader() && data.isNBT() && !data.hasLegacy(prefix + "speedB")) {
            speed = data.as(Share.VISIBLE, "sc").putByte(speed);
        } else {
            speed = data.as(Share.VISIBLE, prefix + "speedB").putByte(speed);
        }
        return this;
    }

    @Override
    protected Object getRecipeItem() {
        return new ItemStack(Items.sugar);
    }

    @Override
    public void motorHit(ServoMotor motor) {
        byte origSpeed = motor.getTargetSpeed();
        motor.setTargetSpeed(speed);
        if (origSpeed != motor.getTargetSpeed()) {
            motor.markDirty();
        }
    }

    @Override
    public String getName() {
        return "fz.instruction.setspeed";
    }

    static IFlatModel[] speeds = new IFlatModel[5];
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return speeds[speed - 1];
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        for (int i = 0; i < 5; i++) {
            speeds[i] = reg(maker, "setspeed/speed_" + i);
        }
    }

    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        speed++;
        if (speed == 6) {
            speed = 1;
        }
        return true;
    }
    
    @Override
    public String getInfo() {
        switch (speed) {
        default:
        case 1: return "Slowest";
        case 2: return "Slow";
        case 3: return "Normal";
        case 4: return "Fast";
        case 5: return "Faster";
        }
    }
    
    @Override
    public CpuBlocking getBlockingBehavior() {
        return CpuBlocking.BLOCK_FOR_TICK;
    }
}
