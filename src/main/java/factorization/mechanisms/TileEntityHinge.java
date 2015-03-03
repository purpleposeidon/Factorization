package factorization.mechanisms;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.BlockClass;
import factorization.shared.BlockRenderHelper;
import factorization.shared.EntityReference;
import factorization.shared.TileEntityCommon;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class TileEntityHinge extends TileEntityCommon implements IDCController {
    FzOrientation facing = FzOrientation.FACE_EAST_POINT_DOWN;
    EntityReference<IDeltaChunk> idcRef;

    @Override
    public void setWorldObj(World world) {
        super.setWorldObj(world);
        if (idcRef == null) {
            idcRef = new EntityReference<IDeltaChunk>(world);
        }
    }

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
        initHinge();
    }

    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        initHinge();
    }

    private void initHinge() {
        if (idcRef != null) return;
        Coord target = getCoord().add(facing.facing);
        if (target.isReplacable()) return;
        DeltaCoord size = new DeltaCoord(16, 16, 16);
        IDeltaChunk idc = DeltaChunk.allocateSlice(this.worldObj, MechanismsFeature.deltachunk_channel, size);
        idc.loadUsualCapabilities();
        idc.permit(DeltaCapability.COLLIDE_WITH_WORLD);
        Coord dest = idc.getCenter();
        TransferLib.move(target, dest, true, true);
        DeltaCoord hingePoint = dest.difference(idc.getCorner());
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        facing = data.as(Share.VISIBLE, "facing").putEnum(facing);
        if (data.as(Share.VISIBLE, "hasRef").putBoolean(idcRef != null)) {
            data.as(Share.VISIBLE, "ref").put(idcRef);
        }
    }

    @Override
    public void setBlockBounds(Block b) {
        float d = 0.5F;
        switch (facing.facing) {
            case DOWN:
                b.setBlockBounds(0, d, 0, 1, 1, 1);
                break;
            case UNKNOWN:
            case UP:
                b.setBlockBounds(0, 0, 0, 1, d, 1);
                break;
            case NORTH:
                b.setBlockBounds(0, 0, d, 1, 1, 1);
                break;
            case SOUTH:
                b.setBlockBounds(0, 0, 0, 1, 1, d);
                break;
            case WEST:
                b.setBlockBounds(d, 0, 0, 1, 1, 1);
                break;
            case EAST:
                b.setBlockBounds(0, 0, 0, d, 1, 1);
                break;
        }
    }

    @Override public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
    @Override public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
    @Override public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }

    @Override
    public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        if (!player.isSprinting()) return false;
        return false;
    }

    IDeltaChunk getIdc() {
        if (idcRef == null) return null;
        return idcRef.getEntity();
    }

    @SideOnly(Side.CLIENT)
    public void renderTesr(float partial) {
        IDeltaChunk idc = getIdc();
        BlockRenderHelper block = BlockRenderHelper.instance;
        setBlockBounds(block);
        ForgeDirection dir = facing.facing;
        float d = 0.5F;
        GL11.glTranslatef(dir.offsetX * d, dir.offsetY * d, dir.offsetZ * d);
        if (idc != null) {
            idc.getRotation().glRotate();
        }
        block.renderForTileEntity();
    }
}
