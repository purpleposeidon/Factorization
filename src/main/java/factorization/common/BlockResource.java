package factorization.common;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.shared.Core;

public class BlockResource extends Block {
    public IIcon[] icons = new IIcon[ResourceType.values().length];
    protected BlockResource() {
        super(Material.rock);
        setHardness(2.0F);
        setBlockName("factorization.ResourceBlock");
    }
    
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        for (ResourceType rt : ResourceType.values()) {
            if (rt.texture == null) {
                continue;
            }
            icons[rt.md] = Core.texture(reg, rt.texture);
        }
        Core.registry.steamFluid.setIcons(BlockIcons.steam);
    }

    
    boolean done_spam = false;
    @Override
    public IIcon getIcon(int side, int md) {
        if (md < icons.length && md >= 0) {
            return icons[md];
        }
        return BlockIcons.error;
    }
    
    public void addCreativeItems(List itemList) {
        itemList.add(Core.registry.silver_ore_item);
        itemList.add(Core.registry.silver_block_item);
        itemList.add(Core.registry.lead_block_item);
        itemList.add(Core.registry.dark_iron_block_item);
    }
    @Override
    public void getSubBlocks(Item par1, CreativeTabs par2CreativeTabs, List itemList) {
        addCreativeItems((List) itemList);
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
    public boolean isBeaconBase(IBlockAccess worldObj, int x, int y, int z,
            int beaconX, int beaconY, int beaconZ) {
        int md = worldObj.getBlockMetadata(x, y, z);
        return md == Core.registry.silver_block_item.getItemDamage()
                || md == Core.registry.lead_block_item.getItemDamage()
                || md == Core.registry.dark_iron_block_item.getItemDamage();
    }
}
