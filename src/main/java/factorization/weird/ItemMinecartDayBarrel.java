package factorization.weird;

import factorization.shared.Core;
import factorization.shared.ItemFactorization;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/*
 * Created by asie on 6/11/15.
 */
public class ItemMinecartDayBarrel extends ItemFactorization {
    public ItemMinecartDayBarrel() {
        super("barrelCart", Core.TabType.TOOLS);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World w, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
        if (!w.isRemote) {
            EntityMinecartDayBarrel minecart = new EntityMinecartDayBarrel(w, x + 0.5F, y + 0.5F, z + 0.5F);
            minecart.initFromStack(is);
            w.spawnEntityInWorld(minecart);
        }
        return true;
    }
}
