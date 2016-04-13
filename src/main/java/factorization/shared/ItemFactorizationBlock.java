package factorization.shared;

import factorization.api.Coord;
import factorization.charge.enet.TileEntityLeydenJar;
import factorization.common.FactoryType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import java.util.List;

public class ItemFactorizationBlock extends ItemBlock {
    public ItemFactorizationBlock(Block id) {
        super(id);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public FactoryType getFT(ItemStack is) {
        return ((BlockFactorization) block).getFactoryType(is.getItemDamage());
    }

    @Override
    public boolean placeBlockAt(ItemStack is, EntityPlayer player,
            World w, BlockPos pos, EnumFacing side, float hitX, float hitY,
            float hitZ, IBlockState newState) {
        Coord here = new Coord(w, pos);
        FactoryType f = getFT(is);
        if (f == null) {
            is.stackSize = 0;
            return false;
        }
        TileEntityCommon rep = f.getRepresentative();
        if (rep == null) return false;
        here.setAsTileEntityLocation(rep);
        Coord placedAgainst = here.add(side.getOpposite());
        boolean good = rep.canPlaceAgainst(player, placedAgainst, side);
        if (!good) {
            return false;
        }
        if (super.placeBlockAt(is, player, w, pos, side, hitX, hitY, hitZ, newState)) {
            TileEntity built = here.getTE();
            TileEntityCommon tec;
            if (built instanceof TileEntityCommon && (((TileEntityCommon) built).getFactoryType() == f)) {
                tec = (TileEntityCommon) built;
            } else {
                tec = f.makeTileEntity();
                here.setAsTileEntityLocation(tec);
            }
            if (tec == null) return false;
            tec.onPlacedBy(player, is, side, hitX, hitY, hitZ);
            tec.getBlockClass().enforce(here);

            here.markBlockForUpdate();
            return true;
        }
        return false;
    }

    @Override
    public int getMetadata(int i) {
        return 15;
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        FactoryType ft = getFT(is);
        return "factorization.factoryBlock." + ft;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List<String> infoList, boolean verbose) {
        if (this == Core.registry.leydenjar_item.getItem()) {
            int perc = 0;
            if (is.hasTagCompound()) {
                FactoryType ft = FactoryType.LEYDENJAR;
                TileEntityLeydenJar jar = (TileEntityLeydenJar) ft.getRepresentative();
                jar.loadFromStack(is);
                perc = (int)(jar.getLevel()*100);
            }
            infoList.add(StatCollector.translateToLocalFormatted("factorization.factoryBlock.LEYDENJAR.perc", perc));
        }
        Core.brand(is, player, infoList, verbose);
    }
    
    @Override
    public boolean getShareTag() {
        return true;
    }
}
