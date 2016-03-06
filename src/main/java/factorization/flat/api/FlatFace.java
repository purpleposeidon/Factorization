package factorization.flat.api;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.FlatMod;
import factorization.flat.FlatNet;
import factorization.util.*;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.IOException;

/** This class contains internal things. */
public abstract class FlatFace implements IDataSerializable {
    @Nullable
    public abstract IFlatModel getModel(Coord at, EnumFacing side);
    public abstract void loadModels(IModelMaker maker);

    public int getColor(Coord at, EnumFacing side) {
        return 0xFFFFFFFF;
    }

    public void onReplaced(Coord at, EnumFacing side) {
        NORELEASE.fixme("Gets called during Slab upgrading");
    }

    public void onPlaced(Coord at, EnumFacing side, EntityPlayer player, ItemStack is) {
        if (!at.w.isRemote) {
            FlatNet.fx(at, side, this, FlatNet.FX_PLACE);
        }
    }

    public boolean isValidAt(Coord at, EnumFacing side) {
        return true;
    }

    public boolean isReplaceable(Coord at, EnumFacing side) {
        return false;
    }

    public void onNeighborBlockChanged(Coord at, EnumFacing side) {
        if (at.w.isRemote) return;
        if (isValidAt(at, side)) return;
        dropItem(at, side);
        Flat.setAir(at, side);
    }

    public void onNeighborFaceChanged(Coord at, EnumFacing side) {

    }

    public void onActivate(Coord at, EnumFacing side, EntityPlayer player) {

    }

    public void onHit(Coord at, EnumFacing side, EntityPlayer player) {
        if (at.w.isRemote) return;
        if (!PlayerUtil.isPlayerCreative(player)) {
            dropItem(at, side);
        }
        Flat.setAir(at, side);
        Flat.playSound(at, side, this);
        Flat.emitParticle(at, side, this);
    }


    public boolean canInteract(Coord at, EnumFacing side, EntityPlayer player) {
        ItemStack item = getItem(at, side);
        if (item == null) return true;
        if (ItemUtil.identical(player.getHeldItem(), item)) {
            return true;
        }
        return false;
    }

    public ItemStack getItem(Coord at, EnumFacing side) {
        return null;
    }

    protected static AxisAlignedBB getBounds(Coord at, EnumFacing side, double width, double height) {
        if (SpaceUtil.sign(side) == -1) {
            // This usually won't happen.
            at = at.add(side);
            side = side.getOpposite();
        }
        final double x = at.x;
        final double y = at.y;
        final double z = at.z;
        final double w = width;
        final double I = height;
        final double l = 0.5 - w;
        final double h = 0.5 + w;
        switch (side) {
            default:
            case EAST:  return new AxisAlignedBB(x+1-I, y + l, z + l, x+1+I, y + h, z + h);
            case UP:    return new AxisAlignedBB(x + l, y+1-I, z + l, x + h, y+1+I, z + h);
            case SOUTH: return new AxisAlignedBB(x + l, y + l, z+1-I, x + h, y + h, z+1+I);
        }
    }

    public void listSelectionBounds(Coord at, EnumFacing side, Entity player, IBoxList list) {
        list.add(getBounds(at, side, 0.5 - 1.0 / 16.0, 1.0 / 64.0));
    }

    /**
     *
     * @return The species. This method is provided as a way to avoid instanceof checks. Use with {@link Flat#nextSpeciesId()}
     */
    public int getSpecies() {
        return -1;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    /** @return a FlatFace that is alike this one, but whose fields can be safely modified. */
    public FlatFace cloneDynamic() {
        return this;
    }

    public final FlatFace dupe() {
        if (isStatic()) return this;
        return cloneDynamic();
    }
    public final boolean isStatic() {
        return staticId != FlatMod.DYNAMIC_SENTINEL;
    }

    public final boolean isDynamic() {
        return staticId == FlatMod.DYNAMIC_SENTINEL;
    }

    /** This field is internal. */
    public transient char staticId = FlatMod.DYNAMIC_SENTINEL;

    /** Like Block.isAir, but even more dangerous. */
    public boolean isNull() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    public void spawnParticle(final Coord at, EnumFacing side) {
        // Yikes, this is too much work!
        // ...for now.
        // FIXME: Implement breaking particles
        /*final Minecraft mc = Minecraft.getMinecraft();
        IFlatModel fm = getModel(at, side);
        if (fm == null) return;
        IBakedModel model = fm.getModel(at, side);
        if (model == null) return;
        final TextureAtlasSprite particle = model.getParticleTexture();
        final IParticleFactory factory = new EntityBreakingFX.Factory();
        this.listSelectionBounds(at, side, mc.thePlayer, new IBoxList() {
            @Override
            public void add(AxisAlignedBB box) {
                int particles = (int) box.getAverageEdgeLength();
                for (int i = 0; i < particles; i++) {
                    double x = NumUtil.randRange(at.w.rand, box.minX, box.maxX);
                    double y = NumUtil.randRange(at.w.rand, box.minY, box.maxY);
                    double z = NumUtil.randRange(at.w.rand, box.minZ, box.maxZ);
                    factory.getEntityFX(0, at.w, x, y, z, )
                }
            }
        });*/
    }

    @SideOnly(Side.CLIENT)
    public void playSound(Coord at, EnumFacing side, boolean placed) {
        Block.SoundType sound = getSoundType();
        if (sound == null) return;
        at.w.playSound(at.x + 0.5, at.y + 0.5, at.z + 0.5,
                sound.getPlaceSound(),
                (sound.getVolume() + 1F) / 2F,
                sound.getFrequency() * 0.8F,
                false /* distance delay */);
    }

    public Block.SoundType getSoundType() {
        return Block.soundTypeStone;
    }

    public void dropItem(Coord at, EnumFacing side) {
        if (at.w.isRemote) return;
        if (FzUtil.doTileDrops(at.w)) {
            if (at.isSolid()) {
                Coord n = at.add(side);
                if (!n.isSolid()) {
                    at = n;
                }
            }
            at.spawnItem(getItem(at, side));
        }
    }
}
