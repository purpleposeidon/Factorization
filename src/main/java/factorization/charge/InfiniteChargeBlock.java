package factorization.charge;

import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.api.datahelpers.DataHelper;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import net.minecraft.util.ITickable;

import java.io.IOException;

public class InfiniteChargeBlock extends TileEntityCommon implements IChargeConductor, ITickable {
    Charge charge = new Charge(this);
    
    @Override
    public void update() {
        if (charge.getValue() < 100) {
            charge.setValue(100);
        }
        charge.update();
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CREATIVE_CHARGE;
    }

    @Override
    public void putData(DataHelper data) throws IOException {

    }

    @Override
    public Charge getCharge() {
        return charge;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
}
