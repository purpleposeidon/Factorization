package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.ceramics.BasicGlazes;
import factorization.shared.Core;

public class BlockResource extends Block {
    public IIcon[] icons = new IIcon[ResourceType.values().length];
    protected BlockResource(int id) {
        super(id, Material.rock);
        setHardness(2.0F);
        setUnlocalizedName("factorization.ResourceBlock");
    }
    
    public static final int glaze_md_start = 17; 
    
    @Override
    public void registerIIcons(IIconRegister reg) {
        for (ResourceType rt : ResourceType.values()) {
            if (rt.texture == null) {
                continue;
            }
            icons[rt.md] = Core.texture(reg, rt.texture);
        }
        for (BasicGlazes glaze : BasicGlazes.values()) {
            glaze.icon = Core.texture(reg, "ceramics/glaze/" + glaze.name());
        }
        Core.registry.steamFluid.setIIcons(BlockIcons.steam);
    }

    
    boolean done_spam = false;
    @Override
    public IIcon getIIcon(int side, int md) {
        if (md >= glaze_md_start) {
            int off = md - glaze_md_start;
            if (off < BasicGlazes.values.length) {
                return BasicGlazes.values[off].icon;
            }
            return BlockIcons.error;
        }
        if (md < icons.length && md >= 0) {
            return icons[md];
        }
        return null;
    }
    
    public void addCreativeItems(List itemList) {
        itemList.add(Core.registry.silver_ore_item);
        itemList.add(Core.registry.silver_block_item);
        itemList.add(Core.registry.lead_block_item);
        itemList.add(Core.registry.dark_iron_block_item);
    }
    
    @Override
    public void addCreativeItems(ArrayList itemList) {
        addCreativeItems((List) itemList);
    }

    @Override
    public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List) {
        //addCreativeItems(par3List);
        Core.addBlockToCreativeList(par3List, this);
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer player, int md,
            float vx, float vy, float vz) {
        if (player.isSneaking()) {
            return false;
        }
        Coord here = new Coord(w, x, y, z);
        return false;
    }

    @Override
    public int damageDropped(int i) {
        return i;
    }
    
    @Override
    public boolean isBeaconBase(World worldObj, int x, int y, int z,
            int beaconX, int beaconY, int beaconZ) {
        Coord here = new Coord(worldObj, x, y, z);
        int md = here.getMd();
        return md == Core.registry.silver_block_item.getItemDamage()
                || md == Core.registry.lead_block_item.getItemDamage()
                || md == Core.registry.dark_iron_block_item.getItemDamage();
    }
}
