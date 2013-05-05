package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;

public class BlockResource extends Block {
    public Icon[] icons = new Icon[ResourceType.values().length];
    @SideOnly(Side.CLIENT)
    Icon exoBottom, exoTop;
    protected BlockResource(int id) {
        super(id, Material.rock);
        setHardness(2.0F);
        setUnlocalizedName("factorization.ResourceBlock");
    }
    
    static final int glaze_md_start = 17; 
    
    @Override
    public void registerIcons(IconRegister reg) {
        exoBottom = Core.texture(reg, "exo/modder_bottom");
        exoTop = Core.texture(reg, "exo/modder_top");
        for (ResourceType rt : ResourceType.values()) {
            icons[rt.md] = Core.texture(reg, rt.texture);
        }
        for (BasicGlazes glaze : BasicGlazes.values()) {
            glaze.icon = Core.texture(reg, "ceramics/glaze/" + glaze.name());
        }
    }

    
    boolean done_spam = false;
    @Override
    public Icon getIcon(int side, int md) {
        if (ResourceType.EXOMODDER.is(md)) {
            if (side == 0) {
                return exoBottom;
            }
            if (side == 1) {
                return exoTop;
            }
        }
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
        itemList.add(Core.registry.exoworkshop_item);
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
        if (ResourceType.EXOMODDER.is(here.getMd())) {
            player.openGui(Core.instance, FactoryType.EXOTABLEGUICONFIG.gui, w, x, y, z);
            return true;
        }
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
