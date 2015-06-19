package factorization.docs;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.docs.DocViewer.HistoryPage;
import factorization.notify.Notice;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;

public class ItemDocBook extends ItemFactorization {

    public ItemDocBook(String name, TabType tabType) {
        super(name, tabType);
        setMaxStackSize(1);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            DocumentationModule.registerGenerators();
        }
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world,
            int x, int y, int z, int side,
            float vx, float fy, float fz) {
        if (!world.isRemote) return false;
        if (!player.isSneaking()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        Coord at = new Coord(world, x, y, z);
        ItemStack hit = at.getPickBlock(mc.objectMouseOver);
        DocumentationModule.tryOpenBookForItem(hit);
        if (hit != null) {
            Notice.onscreen(player, "%s", hit.getDisplayName());
        }
        return true;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        if (!world.isRemote) return is;
        Minecraft mc = Minecraft.getMinecraft();
        //HistoryPage ap = DocViewer.popLastPage();
        mc.displayGuiScreen(new DocViewer("factorization"));
        return is;
    }
    
    @Override
    public void onCreated(ItemStack is, World world, EntityPlayer player) {
        DistributeDocs.setGivenBook(player);
    }

}
