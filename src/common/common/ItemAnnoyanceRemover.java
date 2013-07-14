package factorization.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;

public class ItemAnnoyanceRemover extends ItemCraftingComponent { //NORELEASE

    public ItemAnnoyanceRemover(int id, String name) {
        super(id, name);
    }
    
    
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode && !Core.dev_environ) {
            return is;
        }
        if (world.isRemote) {
            return is;
        }
        if (FMLCommonHandler.instance().getSide() != Side.CLIENT) {
            player.addChatMessage("This device is too devastating to use anywhere except in a test world");
            return is;
        }
        for (Entity ent : (Iterable<Entity>)world.loadedEntityList) {
            if (ent instanceof EntityPlayer) {
                continue;
            }
            if (ent instanceof EntityLiving || ent instanceof EntityItem || ent instanceof IProjectile || ent instanceof IMob) {
                ent.setDead();
            }
        }
        world.getWorldInfo().setRaining(false);
        return is;
    }
    
    @Override
    public Icon getIcon(ItemStack stack, int pass) {
        return Item.bone.getIcon(stack, pass);
    }
}
