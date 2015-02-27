package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.io.IOException;

public class TileEntityHinge extends TileEntityCommon {
    Coord mate = new Coord(this);
    FzOrientation facing = FzOrientation.FACE_EAST_POINT_DOWN;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.HINGE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        facing = SpaceUtil.getOrientation(player, side, hitX, hitY, hitZ);
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        mate = data.as(Share.VISIBLE, "mate").put(mate);
        facing = data.as(Share.VISIBLE, "facing").put(facing);
    }
}
