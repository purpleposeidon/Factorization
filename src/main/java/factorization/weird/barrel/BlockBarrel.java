package factorization.weird.barrel;

import factorization.algos.ReservoirSampler;
import factorization.shared.BlockClass;
import factorization.shared.BlockFactorization;
import factorization.shared.Core;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class BlockBarrel extends BlockFactorization {
    public static final Material materialBarrel = new Material(MapColor.woodColor) {{
        setAdventureModeExempt();
    }};
    public BlockBarrel() {
        super(materialBarrel);
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityDayBarrel();
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        return state.withProperty(BLOCK_CLASS, BlockClass.Barrel);
    }

    @Override
    public void getSubBlocks(Item me, CreativeTabs tab, List<ItemStack> itemList) {
        if (todaysBarrels != null) {
            itemList.addAll(todaysBarrels);
            return;
        }
        if (Core.registry.daybarrel == null) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        int doy = cal.get(Calendar.DAY_OF_YEAR) - 1 /* start at 0, not 1 */;

        ReservoirSampler<ItemStack> barrelPool = new ReservoirSampler<ItemStack>(1, new Random(doy));
        todaysBarrels = new ArrayList<ItemStack>();

        for (ItemStack barrel : TileEntityDayBarrel.barrel_items) {
            TileEntityDayBarrel.Type type = TileEntityDayBarrel.getUpgrade(barrel);
            if (type == TileEntityDayBarrel.Type.NORMAL) {
                barrelPool.give(barrel);
            } else if (type == TileEntityDayBarrel.Type.CREATIVE) {
                todaysBarrels.add(barrel);
            }
        }

        TileEntityDayBarrel rep = new TileEntityDayBarrel();
        for (ItemStack barrel : barrelPool.getSamples()) {
            rep.loadFromStack(barrel);
            for (TileEntityDayBarrel.Type type : TileEntityDayBarrel.Type.values()) {
                if (type == TileEntityDayBarrel.Type.CREATIVE) continue;
                if (type == TileEntityDayBarrel.Type.LARGER) continue;
                rep.type = type;
                todaysBarrels.add(rep.getPickedBlock());
            }
        }
    }

    ArrayList<ItemStack> todaysBarrels = null;
}
