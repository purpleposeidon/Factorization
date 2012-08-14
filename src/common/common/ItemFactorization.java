package factorization.common;

import java.util.List;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import factorization.api.Coord;

public class ItemFactorization extends ItemBlock {
    public ItemFactorization() {
        super(Core.registry.factory_block.blockID + Core.block_item_id_offset);
        new Exception().printStackTrace();
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    public ItemFactorization(int id) {
        super(id);
        //Y'know, that -256 is really retarded.
        setMaxDamage(0);
        setHasSubtypes(true);
    }

    @Override
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float vecx, float vecy, float vecz) {
        Coord here = new Coord(w, x, y, z);
        int id = here.getId();
        if (id == Block.snow.blockID) {
            side = 1;
        }
        else if (id != Block.vine.blockID && id != Block.tallGrass.blockID && id != Block.deadBush.blockID)
        {
            if (side == 0) {
                here.y--;
            }
            if (side == 1) {
                here.y++;
            }
            if (side == 2) {
                here.z--;
            }
            if (side == 3) {
                here.z++;
            }
            if (side == 4) {
                here.x--;
            }
            if (side == 5) {
                here.x++;
            }
        }

        FactoryType f = FactoryType.fromMd(is.getItemDamage());
        if (f == null) {
            is.stackSize = 0;
            return false;
        }
        TileEntity te = f.makeTileEntity();
        if (te instanceof TileEntityCommon) {
            boolean good = ((TileEntityCommon) te).canPlaceAgainst(here.copy().towardSide(CubeFace.oppositeSide(side)), side);
            if (!good) {
                return false;
            }
        }

        boolean ret = super.tryPlaceIntoWorld(is, player, w, x, y, z, side, vecx, vecy, vecz);
        if (ret) {
            //create our TileEntityFactorization
            //Coord c = new Coord(w, x, y, z).towardSide(side);

            w.setBlockTileEntity(here.x, here.y, here.z, te);
            if (te instanceof TileEntityCommon) {
                TileEntityCommon tec = (TileEntityCommon) te;
                tec.onPlacedBy(player, is, side);
                tec.getBlockClass().enforce(here);
            }
            if (!w.isRemote) {
                if (te instanceof TileEntityCommon) {
                    Packet p = ((TileEntityCommon) te).getAuxillaryInfoPacket();
                    Core.network.broadcastPacket(null, here, p); //XXX TODO: Is this necessary?
                }
            }
        }
        return ret;
    }

    public int getIconFromDamage(int damage) {
        return Core.registry.factory_block.getBlockTextureFromSideAndMetadata(0, damage);
    }

    public int getMetadata(int i) {
        return 15;
        //return i;
    }

    @Override
    public String getItemNameIS(ItemStack itemstack) {
        //XXX I think this is actually supposed to return localization IDs like "factory.whatever"
        // I don't think this actually gets called...
        int md = itemstack.getItemDamage();
        return "item.factoryBlock" + md;
    }

    @Override
    public String getItemName() {
        return "ItemFactorization";
    }

    //@Override -- Hello, server
    public void addInformation(ItemStack is, List lines) {
        if (FactoryType.BATTERY.is(is.getItemDamage())) {
            NBTTagCompound tag = FactorizationUtil.getTag(is);
            float fullness = 1;
            if (tag.hasKey("storage")) {
                fullness = TileEntityBattery.getFullness(tag.getInteger("storage"));
            }
            lines.add((int) (fullness * 100) + "% charged");
        }
    }
}
