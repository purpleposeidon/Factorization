package factorization.charge;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import factorization.api.datahelpers.DataHelper;
import factorization.api.energy.ContextTileEntity;
import factorization.api.energy.IEnergyNet;
import factorization.api.energy.WorkUnit;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfiniteEnergy extends TileEntityCommon implements ITickable {
    HashMap<ContextTileEntity, List<WorkUnit>> map = null;

    @Override
    public void neighborChanged() {
        map = null;
    }

    @Override
    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
        map = null;
        return true;
    }

    @Override
    public void update() {
        if (map == null) {
            map = Maps.newHashMap();
            for (EnumFacing dir : EnumFacing.VALUES) {
                List<WorkUnit> give = Lists.newArrayList();
                ContextTileEntity context = new ContextTileEntity(this, dir, null);
                for (WorkUnit wu : WorkUnit.getPrototypes()) {
                    if (IEnergyNet.offer(context, wu)) {
                        give.add(wu);
                    }
                }
                if (!give.isEmpty()) {
                    map.put(context, give);
                }
            }
        }

        for (Map.Entry<ContextTileEntity, List<WorkUnit>> entry : map.entrySet()) {
            ContextTileEntity context = entry.getKey();
            List<WorkUnit> units = entry.getValue();
            for (WorkUnit wu : units) {
                IEnergyNet.offer(context, wu);
            }
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.CREATIVE_CHARGE;
    }

    @Override
    public void putData(DataHelper data) throws IOException {

    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }
}
