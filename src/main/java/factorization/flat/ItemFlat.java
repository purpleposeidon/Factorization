package factorization.flat;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import factorization.util.NORELEASE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * This Item is kind of FZ-specific, but it's easy to make your own.
 */
public class ItemFlat extends ItemFactorization {
    public final FlatFace face;

    public ItemFlat(FlatFace face, Core.TabType tabType) {
        super(getName(face), tabType);
        this.face = face;
        NORELEASE.fixme("This package should NOT contain FZ-specific things");
    }

    static String getName(FlatFace face) {
        if (face == null) throw new NullPointerException();
        return Flat.getName(face).getResourcePath();
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumFacing side,
                             float hitX, float hitY, float hitZ) {
        return Flat.tryUsePlacer(playerIn, stack, face.dupe(), new Coord(worldIn, pos), side);
    }
}
