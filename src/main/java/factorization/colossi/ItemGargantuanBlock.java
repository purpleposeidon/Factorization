package factorization.colossi;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;

public class ItemGargantuanBlock extends ItemBlock {

    public ItemGargantuanBlock(Block block) {
        super(block);
        setMaxStackSize(32);
    }
    
    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int metadata) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        if (dir == ForgeDirection.UNKNOWN) return false;
        Coord me = new Coord(world, x, y, z);
        Coord mate = me.add(dir);
        if (!mate.isReplacable()) return false;
        if (!world.canPlaceEntityOnSide(this.field_150939_a, mate.x, mate.y, mate.z, false, side, player, stack)) return false;
        AxisAlignedBB box = mate.aabbFromRange(mate, mate.add(1, 1, 1));
        if (!world.checkNoEntityCollision(box)) return false;
        metadata = side;
        boolean ret = super.placeBlockAt(stack, player, world, me.x, me.y, me.z, side, hitX, hitY, hitZ, dir.ordinal());
        if (!ret) return false;
        return super.placeBlockAt(stack, player, world, mate.x, mate.y, mate.z, side, hitX, hitY, hitZ, dir.getOpposite().ordinal());
    }
}
