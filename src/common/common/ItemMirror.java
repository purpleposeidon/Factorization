package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import net.minecraft.src.forge.ITextureProvider;

public class ItemMirror extends Item implements ITextureProvider {

    protected ItemMirror(int par1) {
        super(par1);
        Core.instance.addName(this, "Reflective Mirror");
        setTextureFile(Core.texture_file_item);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, int x, int y, int z,
            int side) {
        ItemStack proxy = Core.registry.mirror_item_hidden.copy();
        proxy.stackSize = is.stackSize;
        boolean ret = proxy.getItem().onItemUse(proxy, player, w, x, y, z, side);
        is.stackSize = proxy.stackSize;
        return ret;
    }

    //@Override
    public int getIconFromDamage(int par1) {
        return 9;
    }

    @Override
    public String getItemNameIS(ItemStack par1ItemStack) {
        return "Reflective Mirror";
    }

    @Override
    public String getItemName() {
        return "ItemMirror";
    }

}
