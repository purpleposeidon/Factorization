package factorization.beauty;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.notify.Notice;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.shared.TileEntityCommon;
import factorization.util.DataUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntityFluidMill extends TileEntityCommon implements IRotationalEnergySource, IDCController {
    enum Mode {
        BROKEN, WIND, WATER;
    };
    Mode mode = Mode.BROKEN;
    ForgeDirection outputDirection = ForgeDirection.UP;
    boolean dirty = true;
    final EntityReference<IDeltaChunk> idcRef = new EntityReference<IDeltaChunk>();

    @Override
    public void setWorldObj(World w) {
        super.setWorldObj(w);
        idcRef.setWorld(w);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.FLUID_MILL_GEN;
    }

    @Override
    public boolean canConnect(ForgeDirection direction) {
        return direction == this.outputDirection;
    }

    @Override
    public double availableEnergy(ForgeDirection direction) {
        if (direction != outputDirection) return 0;
        return 0;
    }

    @Override
    public double takeEnergy(ForgeDirection direction, double maxPower) {
        if (direction != outputDirection) return 0;
        return 0;
    }

    @Override
    public double getVelocity(ForgeDirection direction) {
        if (direction != outputDirection) return 0;
        return 0;
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        if (!willHarvest) return super.removedByPlayer(player, willHarvest);
        if (PlayerUtil.isPlayerCreative(player) && player.isSneaking()) {
            IDeltaChunk idc = idcRef.getEntity();
            if (idc != null) idc.setDead();
            return super.removedByPlayer(player, willHarvest);
        }
        if (idcRef.trackedAndAlive()) return false;
        return super.removedByPlayer(player, willHarvest);
    }

    @Override
    public boolean isTileEntityInvalid() {
        return this.isInvalid();
    }

    @Override
    public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) {
        dirty = true;
        return false;
    }

    @Override
    public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        dirty = true;
        return false;
    }

    @Override public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
    @Override public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
    @Override public void idcDied(IDeltaChunk idc) { }
    @Override public void beforeUpdate(IDeltaChunk idc) { }
    @Override public void afterUpdate(IDeltaChunk idc) { }
    @Override public boolean onAttacked(IDeltaChunk idc, DamageSource damageSource, float damage) { return false; }

    @Override
    public void putData(DataHelper data) throws IOException {
        mode = data.as(Share.VISIBLE, "millMode").putEnum(mode);
        outputDirection = data.as(Share.VISIBLE, "outputDirection").putEnum(outputDirection);
        dirty = data.as(Share.PRIVATE, "dirty").putBoolean(dirty);
        data.as(Share.PRIVATE, "idcRef").put(idcRef);
    }

    @Override
    public void representYoSelf() {
        super.representYoSelf();
        channel_id = DeltaChunk.getHammerRegistry().makeChannelFor(Core.modId, "fluidMill", channel_id, -1, "waterwheels & windmills");
    }

    static int channel_id = 100;
    static final int MAX_OUT = 4;
    static final int MAX_IN = 3;
    static final int MAX_RADIUS = 5;

    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        if (worldObj.isRemote) return false;
        if (idcRef.trackedAndAlive()) {
            new Notice(this, "fz.fluidmill.alreadyrunning").sendTo(player);
            return true;
        }
        ItemStack held = player.getHeldItem();
        Block block = DataUtil.getBlock(held);
        if (block == null) {
            new Notice(this, "fz.fluidmill.type." + mode.name()).sendTo(player);
            return true;
        }
        if (!(block instanceof BlockFence)) {
            new Notice(this, "fz.fluidmill.nonfence").sendTo(player);
            return true;
        }
        DeltaCoord idcSize = new DeltaCoord(MAX_RADIUS * 2, MAX_OUT + MAX_IN, MAX_RADIUS * 2);
        DeltaCoord offset = new DeltaCoord(MAX_RADIUS, MAX_OUT, MAX_RADIUS);
        IDeltaChunk idc = DeltaChunk.allocateSlice(worldObj, channel_id, idcSize);
        idc.permit(DeltaCapability.BLOCK_PLACE,
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.INTERACT,
                DeltaCapability.ROTATE,
                DeltaCapability.DIE_WHEN_EMPTY);
        idc.forbid(DeltaCapability.COLLIDE_WITH_WORLD,
                DeltaCapability.COLLIDE,
                DeltaCapability.VIOLENT_COLLISIONS,
                DeltaCapability.DRAG);
        idc.setRotationalCenterOffset(offset.toVector().addVector(0.5, 0.5, 0.5));
        Coord at = new Coord(this);
        final ForgeDirection normal = outputDirection.getOpposite();
        at.add(normal);

        FzOrientation fzo = FzOrientation.fromDirection(normal).getSwapped();
        idc.setRotation(Quaternion.fromOrientation(fzo));
        at.setAsEntityLocation(idc);
        //idc.posX -= 0.5;
        //idc.posZ -= 0.5;
        Coord.iterateEmptyBox(idc.getCorner(), idc.getFarCorner(), new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                here.setId(Blocks.stone);
            }
        });
        worldObj.spawnEntityInWorld(idc);
        idcRef.trackEntity(idc);
        return true;
    }
}
