package factorization.flat;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/**
 * This Item is kind of FZ-specific, but it's easy to make your own.
 */
public class ItemFlat extends ItemFactorization {
    public final FlatFace face;

    public ItemFlat(String name, Core.TabType tabType, FlatFace face) {
        super(name, tabType);
        this.face = face;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        return Flat.tryUsePlacer(playerIn, stack, face.dupe(), new Coord(worldIn, pos), side);
    }
}
