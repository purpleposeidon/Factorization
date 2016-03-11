package factorization.chargeapitest;

import factorization.api.Coord;
import factorization.api.energy.ContextTileEntity;
import factorization.api.energy.EnergyCategory;
import factorization.api.energy.IWorker;
import factorization.api.energy.WorkUnit;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.lang.ref.Reference;

class EnergyApiTest {
    static class MyHeater extends TileEntity implements IWorker<ContextTileEntity>, ITickable {
        {
            IWorker.construct(new ContextTileEntity(this));
        }

        int workbufer = 0;
        int max_buffer = 16;
        int work_per_unit = 8;

        @Override
        public Accepted accept(ContextTileEntity context, WorkUnit unit, boolean simulate) {
            if (unit.category != EnergyCategory.ELECTRIC) return Accepted.NEVER;
            if (workbufer > max_buffer - work_per_unit) return Accepted.LATER;
            if (simulate) return Accepted.NOW;
            work_per_unit += work_per_unit;
            return Accepted.NOW;
        }

        @Override
        public void update() {
            Coord target = new Coord(this).add(EnumFacing.UP);
            if (workbufer <= 0) {
                if (target.getTE(TileEntityFurnace.class) != null) {

                }
                return;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            IWorker.invalidate(new ContextTileEntity(this));
        }
    }

    static class YourBattery extends TileEntity {
        int storage = 0;
        int max = 64;
    }

    static class BatteryWorker implements IWorker<ContextTileEntity> {
        @Override
        public Accepted accept(ContextTileEntity context, WorkUnit unit, boolean simulate) {
            YourBattery bat = (YourBattery) context.te;
            if (bat.storage >= bat.max) return Accepted.LATER;
            if (simulate) return Accepted.NOW;
            bat.storage++;
            return Accepted.NOW;
        }
    }


    static {
        ContextTileEntity.adaptTileEntity.register(YourBattery.class, new BatteryWorker());
        Reference r;
    }
}
