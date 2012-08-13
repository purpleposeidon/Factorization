package factorization.common;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;

public class ItemMirror extends Item {

    protected ItemMirror(int par1) {
        super(par1);
        Core.instance.addName(this, "Reflective Mirror");
        setTextureFile(Core.texture_file_item);
    }

    
    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float vecx, float vecy, float vecz) {
        ItemStack proxy = Core.registry.mirror_item_hidden.copy();
        proxy.stackSize = is.stackSize;
        boolean ret = proxy.getItem().tryPlaceIntoWorld(proxy, player, w, x, y, z, side, vecx, vecy, vecz);
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
