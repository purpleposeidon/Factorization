package factorization.charge;

import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.charge.sparkling.EntitySparkling;
import factorization.common.BlockResource;
import factorization.common.FactoryType;
import factorization.common.ResourceType;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatVisitor;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.util.NORELEASE;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.Vec3;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class TileEntityLIghtningRod extends TileEntityCommon implements ITickable, IMeterInfo {
    static int STATIC_PER_STRIKE = 200;
    static int SLOW_RATE = 100;
    int static_buildup = 0;
    boolean powered = false;
    transient EntitySheep sheep = null;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Ceramic;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        static_buildup = data.as(Share.PRIVATE, "staticBuildup").putInt(static_buildup);
        powered = data.as(Share.PRIVATE, "powered").putBoolean(powered);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LIGHTNING_ROD;
    }

    @Override
    public void update() {
        if (worldObj.isRemote) return;
        if (powered) return;
        if (static_buildup >= STATIC_PER_STRIKE) {
            strike();
            return;
        }
        if (powered) return;
        if (sheep == null) {
            if (worldObj.getTotalWorldTime() % SLOW_RATE == 0) {

                static_buildup++;
            }
            return;
        }
        if (sheep.getSheared() || sheep.getHealth() <= 0 || sheep.hurtResistantTime > 0) {
            sheep = null;
            return;
        }
        static_buildup++;
        Vec3 delta = Quaternion.getRotationQuaternionRadians(Math.PI * 40 * static_buildup / (double) STATIC_PER_STRIKE, EnumFacing.UP).applyRotation(new Vec3(1.25, -0.5, 0));
        Vec3 pos = delta.add(new Coord(this).toMiddleVector());
        SpaceUtil.setEntPos(sheep, pos);
        sheep.motionY = 0;
        if (static_buildup % 20 == 0) {
            sheep.playLivingSound();
        }
    }

    @Override
    public void neighborChanged(Block neighbor) {
        if (worldObj.isRemote) return;
        if (neighbor == Blocks.wool) {
            static_buildup += STATIC_PER_STRIKE / 50;
        }
        powered = new Coord(this).isPowered();
        if (powered) {
            static_buildup = 0;
            sheep = null;
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        powered = new Coord(this).isPowered();
    }

    private void strike() {
        Coord at = new Coord(this);
        int r = 4;
        Coord min = at.add3(-r);
        Coord max = at.add3(+r);
        {
            // Spawn lightning
            EntityLightningBolt bolt = new EntityLightningBolt(worldObj, at.x + 0.5, at.y + 0.5, at.z + 0.5);
            worldObj.addWeatherEffect(bolt);
        }

        {
            // Defleece sheep
            if (sheep != null) {
                sheep.setFleeceColor(EnumDyeColor.YELLOW);
                sheep.setSheared(true);
                sheep.setFire(10);
            }
        }

        {
            // Supercharge
            Flat.iterateRegion(min, max, new IFlatVisitor() {
                @Override
                public void visit(Coord at, EnumFacing side, @Nonnull FlatFace face) {
                    if (face instanceof ISuperChargeable) {
                        ((ISuperChargeable) face).superCharge();
                    }
                }
            });
            // (Or could do Coord.iterateChunks \ Flat.iterateDynamic. Meh.)
            Coord.iterateCube(min, max, new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    ISuperChargeable jar = here.getTE(ISuperChargeable.class);
                    if (jar == null) return;
                    jar.superCharge();
                }
            });
        }

        {
            // Spawn sparklings
            Random rng = worldObj.rand;
            int sparkles = 3 + rng.nextInt(4);
            DeltaCoord dc = max.difference(min);
            for (int i = 0; i < sparkles; i++) {
                Coord spot = min.add(rng.nextInt(dc.x), rng.nextInt(dc.y), rng.nextInt(dc.z));
                if (spot.isSolid()) continue;
                EntitySparkling spark = new EntitySparkling(worldObj);
                spot.setAsEntityLocation(spark);
                spark.setSurgeLevel(50);
                worldObj.spawnEntityInWorld(spark);
            }
        }

        {
            // Melt
            IBlockState bs = Core.registry.resource_block.getDefaultState().withProperty(BlockResource.TYPE, ResourceType.COPPER_BLOCK);
            at.set(bs, true);
            Coord up = at.add(EnumFacing.UP);
            if (up.isReplacable()) {
                IBlockState lava = Blocks.flowing_lava.getDefaultState().withProperty(BlockLiquid.LEVEL, 1);
                up.set(lava, true);
            }
            sheep = null;
            static_buildup = 0;
        }
        // TODO: Achievement
    }

    @Override
    public boolean activate(EntityPlayer player, EnumFacing side) {
        if (worldObj.isRemote) return true;
        if (sheep != null) return false;
        Coord at = new Coord(this);
        AxisAlignedBB box = SpaceUtil.newBox(at.add3(-7), at.add3(+7));
        for (Entity ent : worldObj.getEntitiesWithinAABBExcludingEntity(player, box)) {
            if (ent instanceof EntitySheep) {
                EntitySheep s = (EntitySheep) ent;
                if (s.isChild()) continue;
                if (s.getSheared()) continue;
                if (powered) {
                    new Notice(this, "").withStyle(Style.DRAWITEM).withItem(new ItemStack(Blocks.redstone_torch)).sendTo(player);
                    return false;
                }
                if (s.getLeashedToEntity() == player) {
                    sheep.clearLeashed(true, !PlayerUtil.isPlayerCreative(player));
                }
                sheep = s;
                return true;
            }
        }
        return false;
    }

    @Override
    public String getInfo() {
        int perc = static_buildup * 100 / STATIC_PER_STRIKE;
        return perc + "%";
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return false;
    }
}
