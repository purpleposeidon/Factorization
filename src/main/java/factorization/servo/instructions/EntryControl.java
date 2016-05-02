package factorization.servo.instructions;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.Instruction;
import factorization.servo.ServoMotor;
import factorization.servo.TileEntityServoRail;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;

import java.io.IOException;

public class EntryControl extends Instruction {
    public boolean blocking = false;
    
    @Override
    public IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        blocking = data.asSameShare("block").putBoolean(blocking);
        return this;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, EnumFacing side) {
        if (!playerHasProgrammer(player)) {
            return false;
        }
        TileEntityServoRail sr = block.getTE(TileEntityServoRail.class);
        if (sr.priority >= 0) {
            sr.priority = -1;
            blocking = true;
        } else {
            sr.priority = 1;
            blocking = false;
        }
        return true;
    }
    
    @Override
    public void onPlacedOnRail(TileEntityServoRail sr) {
        sr.priority = 1;
        blocking = false;
    }

    @Override
    protected Object getRecipeItem() {
        return "fenceGateWood";
    }

    @Override
    public void motorHit(ServoMotor motor) { }

    @Override
    public String getName() {
        return "fz.instruction.entryControl";
    }
    
    @Override
    public void afterClientLoad(TileEntityServoRail rail) {
        rail.priority = (byte) (blocking ? -1 : 1);
    }

    static IFlatModel yes, no;
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return blocking ? no : yes;
    }

    @Override
    protected void loadModels(IModelMaker maker) {
        yes = reg(maker, "entry_control/yes");
        no = reg(maker, "entry_control/no");
    }

    @Override
    public byte getPriority() {
        return (byte) (blocking ? -1 : +1);
    }
}
