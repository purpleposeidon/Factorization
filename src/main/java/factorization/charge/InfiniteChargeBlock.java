package factorization.charge;

import factorization.api.datahelpers.DataHelper;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Charge;
import factorization.api.IChargeConductor;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;

import java.io.IOException;

public class InfiniteChargeBlock extends TileEntityCommon implements IChargeConductor {
    Charge charge = new Charge(this);
    
    @Override
    public void updateEntity() {
        if (charge.getValue() < 100) {
            charge.setValue(100);
        }
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
    
    @Override
    public IIcon getIcon(ForgeDirection dir) {
        if (dir.offsetY != 0) return BlockIcons.battery_top;
        return Blocks.bedrock.getIcon(0, 0);
    }
}
