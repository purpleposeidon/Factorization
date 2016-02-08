package factorization.twistedblock;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.Quaternion;
import factorization.fzds.BasicTransformOrder;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.Interpolation;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.util.FzUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;

import java.util.List;

public class ItemTwistedBlock extends ItemBlock {
    static final Block darkIron = Core.registry.resource_block;
    static final int darkIronMd = Core.registry.dark_iron_block_item.getItemDamage();

    public ItemTwistedBlock() {
        super(Core.registry.resource_block);
        FzUtil.initItem(this, "twistedBlock", TabType.ART);
        DeltaChunk.assertEnabled();
    }
    
    final int channel = Hammer.instance.hammerInfo.makeChannelFor(Core.name, "twistedBlocks", 10, 64, "Allows placement of blocks at angles");

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState) {
        if (world.isRemote) return false;
        if (player instanceof FakePlayer) return false;
        Coord at = new Coord(world, pos);
        if (world == DeltaChunk.getWorld(world)) {
            // Do the twist!
            IDimensionSlice[] found = DeltaChunk.getSlicesContainingPoint(at);
            if (found == null || found.length != 1) return false;
            IDimensionSlice idc = found[0];
            NBTTagCompound tag = idc.getTag();
            if (!tag.getBoolean("isTwistedBlockIDC")) return false;
            if (idc.hasOrders()) return false;
            EnumFacing orig = SpaceUtil.getOrientation(tag.getInteger("placedSide"));
            int turns = tag.getInteger("turns");
            int d = player.isSneaking() ? -1 : 1;
            turns += d;
            if (turns < 1) turns = 3;
            if (turns > 3) turns = 1;
            tag.setInteger("turns", turns);
            double angle = (2 * Math.PI) * turns / 16.0;
            Quaternion newRotation = Quaternion.getRotationQuaternionRadians(angle, orig);
            BasicTransformOrder.give(idc, newRotation, 40, Interpolation.SMOOTH);
            return false;
        }
        DeltaCoord size = new DeltaCoord(16, 16, 16);
        IDimensionSlice idc = DeltaChunk.allocateSlice(world, channel, size);
        idc.setPartName("TwistedBlock placed by " + player.getName());
        NBTTagCompound tag = idc.getTag();
        at.writeToNBT("placedAgainst", tag);
        tag.setBoolean("isTwistedBlockIDC", true);
        tag.setString("placedByName", player.getName());
        tag.setString("playerByUUID", player.getUniqueID().toString());
        tag.setInteger("placedSide", side.ordinal());
        tag.setInteger("turns", 0);
        for (DeltaCapability forbidden : new DeltaCapability[] {

        }) {
            idc.forbid(forbidden);
        }
        for (DeltaCapability allowed : new DeltaCapability[] {
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.BLOCK_PLACE,
                DeltaCapability.INTERACT,
                DeltaCapability.COLLIDE,
                DeltaCapability.ROTATE,
                DeltaCapability.DIE_WHEN_EMPTY,
                DeltaCapability.DRAG,
                DeltaCapability.ENTITY_PHYSICS
        }) {
            idc.permit(allowed);
        }
        EnumFacing axis = side;
        double amount = Math.toRadians(45);
        idc.getCenter().setIdMd(darkIron, darkIronMd, true);
        TransformData<Pure> transform = idc.getTransform();
        transform.setPos(at.toMiddleVector());

        Vec3 center = transform.getOffset().addVector(0.5, 0.5, 0.5);
        transform.setOffset(center);
        idc.getRealWorld().spawnEntityInWorld(idc.getEntity());

        Quaternion rotation = Quaternion.getRotationQuaternionRadians(amount, axis);
        BasicTransformOrder.give(idc, rotation, 40, Interpolation.SMOOTH);
        at.add(side).spawnItem(Core.registry.dark_iron_sprocket.copy());

        return true;
    }

    @Override
    public void getSubItems(Item itemId, CreativeTabs tab, List<ItemStack> list) {
        list.add(new ItemStack(this));
    }
}
