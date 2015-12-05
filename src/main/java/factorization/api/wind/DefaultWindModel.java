package factorization.api.wind;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class DefaultWindModel implements IWindModel {
    @Override
    public Vec3 getWindPower(World w, BlockPos pos, IWindmill mill) {
        return new Vec3(-1, 0, 0);
    }

    @Override
    public <T extends TileEntity & IWindmill> void registerWindmillTileEntity(T mill) {

    }

    @Override
    public <T extends TileEntity & IWindmill> void deregisterWindmillTileEntity(T mill) {

    }

    @Override
    public <T extends Entity & IWindmill> void registerWindmillEntity(T mill) {

    }

    @Override
    public <T extends Entity & IWindmill> void deregisterWindmillEntity(T mill) {

    }
}
