package factorization.charge;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IReflectionTarget;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.shared.TileEntityCommon;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TileEntityMirror extends TileEntityCommon {
    public Coord reflection_target = null;

    //don't save
    public boolean is_lit = false;
    int next_check = 1;
    //don't save, but *do* share w/ client
    public transient int target_rotation = -99;
    private boolean covered_by_other_mirror = false;
    public byte silver = 1;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        if (reflection_target == null) {
            reflection_target = getCoord();
        }
        reflection_target = data.as(Share.VISIBLE, "target").putIDS(reflection_target);
        if (reflection_target.equals(getCoord())) {
            reflection_target = null;
        } else if (data.isReader()) {
            updateRotation();
        }
        covered_by_other_mirror = data.as(Share.VISIBLE, "covered").putBoolean(covered_by_other_mirror);
        silver = data.as(Share.VISIBLE, "silver").putByte(silver);
    }

    @Override
    public void setWorldObj(World w) {
        super.setWorldObj(w);
        if (reflection_target != null) {
            reflection_target.w = w;
        }
    }

    @Override
    public boolean activate(EntityPlayer entityplayer, EnumFacing side) {
        neighborChanged();
        return false;
    }

    @Override
    public void neighborChanged() {
        next_check = -1;
        IReflectionTarget target = reflection_target == null ? null : reflection_target.getTE(IReflectionTarget.class);
        byte new_silver = countSilver();
        if (new_silver == silver) return;
        if (target == null) {
            silver = new_silver;
            return;
        }
        int oldPower = -getPower();
        silver = new_silver;
        int newPower = getPower();
        target.addReflector(oldPower + newPower);
        broadcastTargetInfoIfChanged(true);
    }

    int getPower() {
        return silver;
    }

    int clipAngle(int angle) {
        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public boolean hasSun() {
        boolean raining = getWorldObj().isRaining() && getWorldObj().getBiomeGenForCoords(pos.getX(), pos.getY()).rainfall > 0;
        if (raining) return false;
        if (worldObj.getSavedLightValue(EnumSkyBlock.Sky, pos.getX(), pos.getY(), pos.getZ()) < 0xF) return false;
        if (covered_by_other_mirror) return false;
        return worldObj.getSunBrightnessFactor(0) > 0.7;
    }

    int last_shared = -1;

    void broadcastTargetInfoIfChanged(boolean force) {
        if (force || getTargetInfo() != last_shared) {
            Coord target = reflection_target == null ? new Coord(this) : reflection_target;
            broadcastMessage(null, MessageType.MirrorDescription, getTargetInfo(), target.x, target.y, target.z, silver);
            last_shared = getTargetInfo();
        }
    }

    int getTargetInfo() {
        return reflection_target == null ? -99 : target_rotation;
    }

    void setRotationTarget(int new_target) {
        if (this.target_rotation != new_target) {
            this.target_rotation = new_target;
        }
    }

    @Override
    public boolean handleMessageFromServer(MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.MirrorDescription) {
            target_rotation = input.readInt();
            reflection_target = new Coord(this);
            reflection_target.x = input.readInt();
            reflection_target.y = input.readInt();
            reflection_target.z = input.readInt();
            silver = input.readByte();
            getCoord().redraw();
            gotten_info_packet = true;
            return true;
        }
        return false;
    }

    void setBelowObscured(Coord at, boolean state) {
        // Don't let mirrors be stacked on top of eachother. A few things to make this work:
        // 1) store this information
        // 2) always cover mirrors below us when placed
        // 3) always uncover mirrors below us when broken, but only if we ourselves aren't covered
        while (at.y > 0) {
            at.y--;
            if (at.getBlock() instanceof BlockFactorization) {
                TileEntityMirror below = at.getTE(TileEntityMirror.class);
                if (below != null) {
                    below.covered_by_other_mirror = state;
                    break;
                }
            }
        }
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, EnumFacing side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        setBelowObscured(getCoord(), true);
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        if (!covered_by_other_mirror) {
            setBelowObscured(getCoord(), false);
        }
        if (reflection_target == null) {
            return;
        }
        reflection_target.w = worldObj;
        if (worldObj == null) {
            return;
        }
        IReflectionTarget target = reflection_target.getTE(IReflectionTarget.class);
        if (target == null) {
            return;
        }
        if (is_lit) {
            target.addReflector(-getPower());
            is_lit = false;
        }
        reflection_target = null;
    }
    
    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null) {
            onRemove();
        }
    }

    boolean gotten_info_packet = false;
    
    void setNextCheck() {
        next_check = 80 + worldObj.rand.nextInt(20);
    }

    public boolean last_drawn_as_lit = false;
    
    @Override
    public void updateEntity() {
        if (next_check-- <= 0) {
            setNextCheck();
            if (worldObj.isRemote) {
                is_lit = hasSun();
                if (is_lit != last_drawn_as_lit && FzConfig.mirror_sunbeams && reflection_target != null) {
                    new Coord(this).markBlockForUpdate();
                }
                return;
            }
            try {
                if (reflection_target == null) {
                    findTarget();
                    if (reflection_target == null) {
                        return;
                    }
                } else {
                    reflection_target.setWorld(worldObj);
                }
                //we *do* have a target coord by this point. Is there a TE there tho?
                IReflectionTarget target = null;
                target = reflection_target.getTE(IReflectionTarget.class);
                if (target == null) {
                    if (reflection_target.blockExists()) {
                        reflection_target = null;
                        is_lit = false;
                    }
                    return;
                }
                if (!myTrace(reflection_target.x, reflection_target.z)) {
                    if (is_lit) {
                        is_lit = false;
                        target.addReflector(-getPower());
                        reflection_target = null;
                        setRotationTarget(-99);
                        return;
                    }
                }

                boolean has_sun = hasSun();
                if (has_sun != is_lit) {
                    is_lit = has_sun;
                    target.addReflector(is_lit ? getPower() : -getPower());
                }
            } finally {
                broadcastTargetInfoIfChanged(false);
            }
        }
    }

    void findTarget() {
        if (reflection_target != null) {
            //make the old target forget about us
            IReflectionTarget target = reflection_target.getTE(IReflectionTarget.class);
            if (target != null) {
                if (is_lit) {
                    target.addReflector(-getPower());
                }
                reflection_target = null;
            }
            is_lit = false;
        }

        int search_distance = 11;
        IReflectionTarget closest = null;
        int last_dist = Integer.MAX_VALUE;
        Coord me = getCoord();
        double maxRadiusSq = 8.9*8.9;
        for (int x = pos.getX() - search_distance; x <= pos.getX() + search_distance; x++) {
            for (int z = pos.getZ() - search_distance; z <= pos.getZ() + search_distance; z++) {
                Coord here = new Coord(worldObj, x, pos.getY(), z);
                IReflectionTarget target = here.getTE(IReflectionTarget.class); // FIXME: Iterate the chunk hash maps instead... get a nice helper function perhaps
                if (target == null) {
                    continue;
                }
                if (!myTrace(x, z)) {
                    continue;
                }
                int new_dist = me.distanceSq(here);
                if (new_dist < last_dist && new_dist <= maxRadiusSq) {
                    last_dist = new_dist;
                    closest = target;
                }
            }
        }
        if (closest != null) {
            reflection_target = closest.getCoord();
            updateRotation();
        } else {
            setRotationTarget(-99);
        }
    }

    void updateRotation() {
        DeltaCoord dc = getCoord().difference(reflection_target);

        int new_target = clipAngle((int) Math.toDegrees(dc.getAngleHorizontal()));
        setRotationTarget(new_target);
    }

    double div(double a, double b) {
        if (b == 0) {
            return Math.signum(a) * 0xFFF;
        }
        return a / b;
    }

    boolean myTrace(double x, double z) {
        x += 0.5;
        z += 0.5;
        double offset_x = x - (pos.getX() + 0.5), offset_z = z - (pos.getZ() + 0.5);
        double length = Math.hypot(offset_x, offset_z);
        double dx = offset_x / length, dz = offset_z / length;
        x -= dx;
        z -= dz;
        int bx = 0, bz = 0;
        for (int i = 0; i < length; i++) {
            bx = (int) Math.round(x + 0.5) - 1;
            bz = (int) Math.round(z + 0.5) - 1;
            if (bx == pos.getX() && bz == pos.getZ()) {
                return true;
            }
            final Block b = worldObj.getBlock(bx, pos.getY(), bz);
            boolean air_like = false;
            if (b == null) {
                air_like = true;
            } else {
                air_like = b.isAir(worldObj, bx, pos.getY(), bz);
                air_like |= b.getCollisionBoundingBoxFromPool(worldObj, bx, pos.getY(), bz) == null;
            }
            if (!air_like) {
                return false;
            }
            x -= dx;
            z -= dz;
        }
        return false;
    }

    @Override
    public boolean isBlockSolidOnSide(EnumFacing side) {
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(EnumFacing dir) {
        return BlockIcons.mirror_front;
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        return new ItemStack(Core.registry.mirror);
    }


    private static ItemStack[] _silver_blocks = null;

    public static ItemStack[] getSilver() {
        if (_silver_blocks != null) return _silver_blocks;
        ArrayList<ItemStack> foundSilver = new ArrayList<ItemStack>();
        for (String oreName : Arrays.asList("blockSilver", "blockGold", "blockCopper", "factorization:mirrorBoost")) {
            for (ItemStack ag : OreDictionary.getOres(oreName)) {
                Block b = DataUtil.getBlock(ag);
                if (b == null) continue;
                foundSilver.add(ag);
            }
        }
        return _silver_blocks = foundSilver.toArray(new ItemStack[foundSilver.size()]);
    }

    private byte countSilver() {
        byte ret = 1;
        for (Coord c : getCoord().getNeighborsAdjacent()) {
            if (c.isAir() || !c.isSolid()) continue;
            ItemStack is = c.getPickBlock(EnumFacing.DOWN);
            for (ItemStack ag : getSilver()) {
                if (ItemUtil.wildcardSimilar(ag, is)) {
                    ret += 2;
                    if (ret > 6) return 6;
                    break;
                }
            }
        }
        return ret;
    }
}
