package factorization.shared;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.ceramics.TileEntityGreenware;
import factorization.ceramics.TileEntityGreenware.ClayState;
import factorization.common.TileEntityLeydenJar;
import factorization.common.TileEntityRocketEngine;

public class ItemFactorizationBlock extends ItemBlock {
    public ItemFactorizationBlock(int id) {
        super(id);
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public boolean placeBlockAt(ItemStack is, EntityPlayer player,
            World w, int x, int y, int z, int side, float hitX, float hitY,
            float hitZ, int md) {
        Coord here = new Coord(w, x, y, z);
        FactoryType f = FactoryType.fromMd(is.getItemDamage());
        if (f == null) {
            is.stackSize = 0;
            return false;
        }
        TileEntity te = f.makeTileEntity();
        if (te instanceof TileEntityCommon) {
            int oppositeSide = ForgeDirection.getOrientation(side).getOpposite().ordinal();
            boolean good = ((TileEntityCommon) te).canPlaceAgainst(player, here.copy().towardSide(oppositeSide), side);
            if (!good) {
                return false;
            }
        }
        if (super.placeBlockAt(is, player, w, x, y, z, side, hitX, hitY, hitZ, md)) {
            //create our TileEntityFactorization
            //Coord c = new Coord(w, x, y, z).towardSide(side);

            if (te instanceof TileEntityCommon) {
                TileEntityCommon tec = (TileEntityCommon) te;
                here.setAsTileEntityLocation(tec);
                tec.onPlacedBy(player, is, side, hitX, hitY, hitZ);
                tec.getBlockClass().enforce(here);
            }
            if (!(te instanceof TileEntityRocketEngine)) {
                w.setBlockTileEntity(here.x, here.y, here.z, te);
            }
            
            here.markBlockForUpdate();
            return true;
        }
        return false;
    }

    @Override
    public Icon getIconFromDamage(int damage) {
        return Core.registry.factory_block.getIcon(0, damage);
    }

    @Override
    public int getMetadata(int i) {
        return 15;
        //return i;
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        int md = is.getItemDamage();
        FactoryType ft = FactoryType.fromMd(md);
        return "factorization.factoryBlock." + ft;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        if (Core.registry.greenware_item != null && is.isItemEqual(Core.registry.greenware_item) /* required to not compare NBT here */) {
            NBTTagCompound tag = is.getTagCompound();
            if (tag != null) {
                TileEntityGreenware teg = (TileEntityGreenware) FactoryType.CERAMIC.getRepresentative();
                teg.readFromNBT(tag);
                ClayState state = teg.getState();
                infoList.add(teg.parts.size() + " parts");
                infoList.add(state.toString());
            }
        }
        if (FzUtil.similar(is, Core.registry.leydenjar_item)) {
            if (is.hasTagCompound()) {
                FactoryType ft = FactoryType.LEYDENJAR;
                TileEntityLeydenJar jar = (TileEntityLeydenJar) ft.getRepresentative();
                jar.onPlacedBy(player, is, 0);
                infoList.add(((int)(jar.getLevel()*100) + "% charged"));
            } else {
                infoList.add("0% charged");
            }
        }
        Core.brand(is, player, infoList, verbose);
    }
    
    @Override
    public boolean getShareTag() {
        return true;
    }
}
