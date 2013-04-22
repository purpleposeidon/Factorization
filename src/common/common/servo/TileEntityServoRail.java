package factorization.common.servo;

import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockClass;
import factorization.common.FactoryType;
import factorization.common.TileEntityCommon;

public class TileEntityServoRail extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(this);
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    @Override
    public Charge getCharge() {
        return charge;
    }
    
    @Override
    public String getInfo() {
        return null;
    }
    
    @Override
    public void updateEntity() {
    }
}
