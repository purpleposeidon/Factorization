package factorization.twistedblock;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.Core.TabType;
import factorization.shared.ItemBlockProxy;

public class ItemTwistedBlock extends ItemBlockProxy {
    static final Block darkIron = Core.registry.resource_block;
    static final int darkIronMd = Core.registry.dark_iron_block_item.getItemDamage();

    public ItemTwistedBlock() {
        super(Core.registry.dark_iron_block_item.copy(), "twistedBlock", TabType.ART);
    }
    
    final int channel = Hammer.instance.hammerInfo.makeChannelFor(Core.name, "twistedBlocks", 10, 64, "Allows placement of blocks at angles");

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ, int metadata) {
        if (world.isRemote) return false;
        if (player instanceof FakePlayer) return false;
        Coord at = new Coord(world, x, y, z);
        if (world == DeltaChunk.getWorld(world)) {
            // Do the twist!
            IDeltaChunk[] found = DeltaChunk.getSlicesContainingPoint(at);
            if (found == null || found.length != 1) return false;
            IDeltaChunk idc = found[0];
            NBTTagCompound tag = idc.getEntityData();
            if (!tag.getBoolean("isTwistedBlockIDC")) return false;
            if (idc.hasOrderedRotation()) return false;
            ForgeDirection orig = ForgeDirection.getOrientation(tag.getInteger("placedSide"));
            int turns = tag.getInteger("turns");
            int d = player.isSneaking() ? -1 : 1;
            turns += d;
            if (turns < 1) turns = 3;
            if (turns > 3) turns = 1;
            tag.setInteger("turns", turns);
            double angle = (2 * Math.PI) * turns / 16.0;
            Quaternion newRotation = Quaternion.getRotationQuaternionRadians(angle, orig);
            idc.orderTargetRotation(newRotation, 60, Interpolation.SMOOTH);
            return false;
        }
        DeltaCoord size = new DeltaCoord(16, 16, 16);
        IDeltaChunk idc = DeltaChunk.allocateSlice(world, channel, size);
        idc.setPartName("TwistedBlock placed by " + player.getCommandSenderName());
        NBTTagCompound tag = idc.getEntityData();
        at.writeToNBT("placedAgainst", tag);
        tag.setBoolean("isTwistedBlockIDC", true);
        tag.setString("placedByName", player.getCommandSenderName());
        tag.setString("playerByUUID", player.getUniqueID().toString());
        tag.setInteger("placedSide", side);
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
        ForgeDirection axis = ForgeDirection.getOrientation(side);
        double amount = Math.toRadians(45);
        idc.getCenter().setIdMd(darkIron, darkIronMd, true);
        idc.posX = at.x + 0.5;
        idc.posY = at.y + 0.5;
        idc.posZ = at.z + 0.5;
        
        Vec3 center = idc.getRotationalCenterOffset().addVector(0.5, 0.5, 0.5);
        idc.setRotationalCenterOffset(center);
        idc.worldObj.spawnEntityInWorld(idc);

        Quaternion rotation = Quaternion.getRotationQuaternionRadians(amount, axis);
        idc.orderTargetRotation(rotation, 20 * 16, Interpolation.SMOOTH3);

        return true;
    }
    
    @Override
    public void getSubItems(Item itemId, CreativeTabs tab, List list) {
        list.add(new ItemStack(this));
    }

    @Override
    public void registerIcons(IIconRegister registry) { }
}
