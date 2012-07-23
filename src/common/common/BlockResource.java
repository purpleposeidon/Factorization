package factorization.common;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Material;
import net.minecraft.src.World;
import net.minecraft.src.forge.ITextureProvider;
import factorization.api.Coord;

public class BlockResource extends Block implements ITextureProvider {
    protected BlockResource(int id) {
        super(id, Material.rock);
        setHardness(2.0F);
    }

    @Override
    public int getBlockTextureFromSideAndMetadata(int side, int md) {
        if (ResourceType.MECHAMODDER.is(md)) {
            if (side == 0) {
                return Texture.mecha_bottom;
            }
            if (side == 1) {
                return Texture.mecha_config;
            }
            return Texture.mecha_side;
        }
        return (16 * 2) + md;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_block;
    }

    @Override
    public String getBlockName() {
        return "Factorization Resource";
    }

    @Override
    public void addCreativeItems(ArrayList itemList) {
        itemList.add(Core.registry.silver_ore_item);
        itemList.add(Core.registry.silver_block_item);
        itemList.add(Core.registry.lead_block_item);
        itemList.add(Core.registry.dark_iron_block_item);
        itemList.add(Core.registry.mechaworkshop_item);
    }

    @Override
    public boolean blockActivated(World w, int x, int y, int z, EntityPlayer player) {
        if (player.isSneaking()) {
            return false;
        }
        Coord here = new Coord(w, x, y, z);
        if (ResourceType.MECHAMODDER.is(here.getMd())) {
            player.openGui(Core.instance, FactoryType.MECHATABLEGUICONFIG.gui, w, x, y, z);
            return true;
        }
        return false;
    }

    @Override
    protected int damageDropped(int i) {
        return i;
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return 1;
    }
}
