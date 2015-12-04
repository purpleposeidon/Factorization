package factorization.mechanics;

import factorization.api.Coord;
import factorization.common.ItemIcons;
import factorization.fzds.DeltaChunk;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.notify.Notice;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.Arrays;
import java.util.List;

public class ItemDarkIronChain extends ItemFactorization {
    public ItemDarkIronChain(String name, Core.TabType tabType) {
        super(name, tabType);
        setMaxStackSize(1); // Would've preferred 16, but it'd behave wonkily with the NBT
    }

    @Override
    public boolean onItemUseFirst(ItemStack is, EntityPlayer player, World world, BlockPos pos, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;
        Coord at = new Coord(world, x, y, z);
        if (player.isSneaking()) {
            clearPos(is, "real");
            clearPos(is, "shadow");
            new Notice(at, "item.factorization:darkIronChain.clear").sendTo(player);
            return true;
        }
        EnumFacing dir = SpaceUtil.getOrientation(side);
        if (world == DeltaChunk.getServerShadowWorld()) {
            for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(at)) {
                if (!acceptableIDC(idc)) continue;
                if (!at.isSolidOnSide(dir)) continue;
                setPos(is, "shadow", at, dir);
                if (bothSet(is)) {
                    connectChain(player, is);
                } else {
                    new Notice(at, "item.factorization:darkIronChain.setShadow").sendTo(player);
                }
                return true;
            }
        } else {
            if (acceptableAnchor(at)) {
                setPos(is, "real", at, dir);
                if (bothSet(is)) {
                    connectChain(player, is);
                } else {
                    new Notice(at, "item.factorization:darkIronChain.setReal").sendTo(player);
                }
                return true;
            } else {
                new Notice(at, "item.factorization:darkIronChain.invalid").sendTo(player);
            }
        }
        return false;
    }

    boolean acceptableAnchor(Coord at) {
        SocketPoweredCrank crank = at.getTE(SocketPoweredCrank.class);
        if (crank == null) return false;
        return !crank.isChained();
    }

    boolean acceptableIDC(IDeltaChunk idc) {
        if (!MechanicsController.usable(idc)) return false;
        for (DeltaCapability req : new DeltaCapability[] {
                DeltaCapability.INTERACT,
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.BLOCK_PLACE,
                DeltaCapability.MOVE,
                DeltaCapability.COLLIDE,
                DeltaCapability.COLLIDE_WITH_WORLD
        }) {
            if (!idc.can(req)) return false;
        }
        return true;
    }

    void setPos(ItemStack is, String name, Coord at, EnumFacing side) {
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.setInteger(name + ":w", FzUtil.getWorldDimension(at.w));
        tag.setInteger(name + ":x", at.x);
        tag.setInteger(name + ":y", at.y);
        tag.setInteger(name + ":z", at.z);
        tag.setByte(name + ":s", (byte) side.ordinal());
    }

    void clearPos(ItemStack is, String name) {
        if (!is.hasTagCompound()) return;
        NBTTagCompound tag = ItemUtil.getTag(is);
        tag.removeTag(name + ":w");
        tag.removeTag(name + ":x");
        tag.removeTag(name + ":y");
        tag.removeTag(name + ":z");
        tag.removeTag(name + ":s");
        if (tag.hasNoTags()) {
            is.setTagCompound(null);
        }
    }

    Coord loadPos(ItemStack is, String name) {
        if (!is.hasTagCompound()) return null;
        NBTTagCompound tag = ItemUtil.getTag(is);
        if (!tag.hasKey(name + ":w")) return null;
        World w = DimensionManager.getWorld(tag.getInteger(name + ":w"));
        if (w == null) return null;
        int x = tag.getInteger(name + ":x");
        int y = tag.getInteger(name + ":y");
        int z = tag.getInteger(name + ":z");
        return new Coord(w, x, y, z);
    }

    EnumFacing loadSide(ItemStack is, String name) {
        if (!is.hasTagCompound()) return null;
        NBTTagCompound tag = ItemUtil.getTag(is);
        if (!tag.hasKey(name + ":s")) return null;
        byte b = tag.getByte(name + ":s");
        return SpaceUtil.getOrientation(b);
    }

    boolean bothSet(ItemStack is) {
        Coord real = loadPos(is, "real");
        Coord shadow = loadPos(is, "shadow");
        return real != null && shadow != null;
    }

    void connectChain(EntityPlayer player, ItemStack is) {
        Coord real = loadPos(is, "real");
        Coord shadow = loadPos(is, "shadow");
        if (real == null || shadow == null) return;
        SocketPoweredCrank crank = real.getTE(SocketPoweredCrank.class);
        if (crank == null || !acceptableAnchor(real)) {
            killChain(player, is);
            return;
        }
        IDeltaChunk toHook = null;
        for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(shadow)) {
            if (!acceptableIDC(idc)) continue;
            toHook = idc;
            // Would probably be best to hook the IDC closest to the player... or maybe, you know, the one actually hit.
        }
        if (toHook == null) {
            killChain(player, is);
            return;
        }
        Coord fake = shadow.copy();
        toHook.shadow2real(fake);
        if (real.distance(fake) > SocketPoweredCrank.MAX_CHAIN_LEN - 2) {
            Notice.onscreen(player, "item.factorization:darkIronChain.tooLong");
            killChain(player, is);
            return;
        }
        double d = 0.5;
        final Vec3 anchorPoint = shadow.createVector().addVector(d, d, d);
        EnumFacing dir = loadSide(is, "shadow");
        Vec3 dv = SpaceUtil.scale(SpaceUtil.fromDirection(dir), 0.5);
        SpaceUtil.incrAdd(anchorPoint, dv);

        crank.setChain(toHook, anchorPoint, shadow);
        Notice.onscreen(player, "item.factorization:darkIronChain.finish"); // Not really necessary, since you should be able to see the chain
        if (player.capabilities.isCreativeMode) {
            is.setTagCompound(new NBTTagCompound());
        } else {
            is.stackSize--;
        }
        for (Coord at : Arrays.asList(real, fake)) {
            at.w.playSound(at.x, at.y, at.z, "factorization:winch.unwind", 1F, 1F, false);
        }

    }

    void killChain(EntityPlayer player, ItemStack is) {
        clearPos(is, "real");
        clearPos(is, "shadow");
        Notice.onscreen(player, "item.factorization:darkIronChain.endLost");
    }

    @Override
    public IIcon getIcon(ItemStack stack, int pass) {
        if (stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();
            if (tag.hasKey("real:w") || tag.hasKey("shadow:w")) {
                return ItemIcons.darkIronChainHalf;
            }
        }
        return super.getIcon(stack, pass);
    }

    @Override
    public boolean hasEffect(ItemStack is, int pass) {
        if (is.hasTagCompound()) return true;
        return super.hasEffect(is, pass);
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        if (!Core.dev_environ) return;
        Coord real = loadPos(is, "real");
        Coord shadow = loadPos(is, "shadow");
        if (real != null) {
            list.add("Real: " + real.toShortString());
        }
        if (shadow != null) {
            list.add("Shadow: " + shadow.toShortString());
        }
    }
}
