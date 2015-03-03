package factorization.mechanisms;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.notify.Notice;
import factorization.shared.*;
import factorization.sockets.SocketLacerator;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

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
    private long ticks;

    @SideOnly(Side.CLIENT)
    @Override
    public void updateEntity() {
        // Client-only!
        ticks++;
        MovingObjectPosition mop = Minecraft.getMinecraft().getObjectMouseOver();
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;
        if (mop.blockX == xCoord && mop.blockY == yCoord && mop.blockZ == zCoord) {
            ticks = 0;
        }
    }

    @Override
    public boolean canUpdate() {
        return worldObj.isRemote;
    }

    @SideOnly(Side.CLIENT)
    public static ObjectModel hingeTop;

    @Override
    @SideOnly(Side.CLIENT)
    public void representYoSelf() {
        super.representYoSelf();
        hingeTop = new ObjectModel(Core.getResource("models/hingeTop.obj"));
    }

    @SideOnly(Side.CLIENT)
    public void renderTesr(float partial) {
        IDeltaChunk idc = getIdc();
        BlockRenderHelper block = BlockRenderHelper.instance;

        setupHingeRotation2();

        if (idc != null) {
            idc.getRotation().glRotate();
        } else {
            float nowish = ticks + partial;
            double now = (Math.cos(nowish / 24.0) - 1) * -6;
            GL11.glRotated(now, 0, 0, 1);
        }

        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        glEnable(GL_LIGHTING);
        glDisable(GL11.GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        hingeTop.render(BlockIcons.mechanism$hinge_uvs);
        glEnable(GL11.GL_CULL_FACE);
        glEnable(GL_LIGHTING);
    }

    private void setupHingeRotation2() {
        // Well, it's better than my 125 line first try. I bet Player could do better tho.
        final ForgeDirection face = facing.facing;
        final ForgeDirection top = facing.top;
        final int fsign = face.ordinal() % 2 == 0 ? -1 : +1;
        final int tsign = top.ordinal() % 2 == 0 ? -1 : +1;
        float dx = 0, dy = 0, dz = 0;
        if (tsign == +1) {
            ForgeDirection v = top;
            dx += v.offsetX; dy += v.offsetY; dz += v.offsetZ;
        }
        if (fsign == +1) {
            ForgeDirection v = facing.rotateOnFace(1).top;
            dx += v.offsetX; dy += v.offsetY; dz += v.offsetZ;
        }
        GL11.glTranslatef(dx, dy, dz);
        Quaternion.fromOrientation(facing).glRotate();
        boolean left = false;
        if (face.offsetX != 0) left = top == ForgeDirection.NORTH || top == ForgeDirection.UP;
        if (face.offsetY != 0) left = top == ForgeDirection.WEST || top == ForgeDirection.SOUTH;
        if (face.offsetZ != 0) left = top == ForgeDirection.DOWN || top == ForgeDirection.EAST;

        float dleft = 0.5F;
        if (left) {
            dleft += fsign;
        }
        GL11.glTranslatef(0, 0.5F * fsign, dleft);
    }
}
