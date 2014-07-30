package factorization.colossi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import factorization.common.BlockIcons;
import factorization.shared.Core;
import factorization.shared.Core.TabType;

public class ColossalBlock extends Block {
    static Material collosal_material = new Material(MapColor.purpleColor);
    
    public ColossalBlock() {
        super(collosal_material);
        setHardness(-1);
        setResistance(500);
        setStepSound(soundTypePiston);
        setBlockName("factorization:colossalBlock");
        Core.tab(this, TabType.BLOCKS);
    }
    
    static final byte MD_BODY = 0, MD_BODY_CRACKED = 1, MD_ARM = 2, MD_LEG = 3, MD_MASK = 4, MD_EYE = 5, MD_CORE = 6;
    
    @Override
    public IIcon getIcon(int side, int md) {
        switch (md) {
        case MD_BODY: return BlockIcons.colossi$body;
        case MD_BODY_CRACKED: return BlockIcons.colossi$body_cracked;
        case MD_ARM: return BlockIcons.colossi$arm;
        case MD_LEG: return BlockIcons.dark_iron_block;
        case MD_MASK: return Blocks.obsidian.getBlockTextureFromSide(0);
        case MD_EYE: return Blocks.diamond_block.getBlockTextureFromSide(0);
        case MD_CORE: return Blocks.redstone_block.getBlockTextureFromSide(0);
        default: return super.getIcon(side, md);
        } 
    }
    
    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        if (world.getBlockMetadata(x, y, z) == MD_BODY_CRACKED) {
            return 10;
        }
        return super.getBlockHardness(world, x, y, z);
    }
    
    @Override
    public void registerBlockIcons(IIconRegister iconRegistry) { }
    
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (byte md = MD_BODY; md <= MD_CORE; md++) {
            list.add(new ItemStack(this, 1, md));
        }
    }
    
    ChestGenHooks core = new ChestGenHooks("factorization:colossalCore");
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList();
        
        return ret;
    }
}
