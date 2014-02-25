package factorization.docs;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;

public class ItemDocBook extends ItemFactorization {

    public ItemDocBook(int itemId, String name, TabType tabType) {
        super(itemId, name, tabType);
    }
    
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world,
            int x, int y, int z, int side,
            float vx, float fy, float fz) {
        return super.onItemUse(is, player, world, x, y, z, side, vx, fy, fz);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        if (!world.isRemote) return is;
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new DocViewer(DocViewer.current_page));
        return is;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getIconFromDamage(int par1) {
        return Item.book.getIconFromDamage(0);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack is, int pass) {
        return 0xFF40FF;
    }

}
