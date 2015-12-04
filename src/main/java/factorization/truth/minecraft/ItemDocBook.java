package factorization.truth.minecraft;

import factorization.api.Coord;
import factorization.notify.Notice;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.truth.DocViewer;
import factorization.truth.api.DocReg;
import factorization.truth.api.IDocBook;
import factorization.util.FzUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemDocBook extends ItemFactorization implements IDocBook {

    public ItemDocBook(String name, TabType tabType) {
        super(name, tabType);
        setMaxStackSize(1);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            DocReg.indexed_domains.add(getDocumentationDomain());
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
        ItemStack hit = FzUtil.getReifiedBarrel(at);
        if (hit == null) {
            hit = at.getPickBlock(mc.objectMouseOver);
        }
        if (hit != null && DocReg.module != null) {
            DocReg.module.openBookForItem(hit, false);
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
        mc.displayGuiScreen(new DocViewer(getDocumentationDomain()));
        return is;
    }
    
    @Override
    public void onCreated(ItemStack is, World world, EntityPlayer player) {
        DistributeDocs.setGivenBook(player);
    }

    @Override
    public String getDocumentationDomain() {
        return "factorization";
    }
}
