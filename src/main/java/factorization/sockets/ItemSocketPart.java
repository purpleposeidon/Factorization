package factorization.sockets;

import factorization.common.FactoryType;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.util.LangUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@Deprecated // We want to work with all items.
public class ItemSocketPart extends ItemFactorization {

    public ItemSocketPart(String name, TabType tabType) {
        super(name, tabType);
        setHasSubtypes(true);
        setMaxDamage(0);
    }
    
    
    ArrayList<FactoryType> loadSockets() {
        ArrayList<FactoryType> ret = new ArrayList();
        for (FactoryType ft : FactoryType.values()) {
            if (ft == FactoryType.SOCKET_EMPTY) {
                continue;
            }
            Class theClass = ft.getFactoryTypeClass();
            while (theClass != null) {
                theClass = theClass.getSuperclass();
                if (theClass == TileEntitySocketBase.class) {
                    TileEntitySocketBase ts = (TileEntitySocketBase) ft.getRepresentative();
                    ItemStack is = ts.getCreatingItem();
                    if (is == null) {
                        break;
                    }
                    if (is.getItem() == this) {
                        ret.add(ft);
                        break;
                    }
                }
            }
        }
        return ret;
    }
    
    FactoryType[] socketTypes = null;
    FactoryType[] getSockets() {
        if (socketTypes == null) {
            ArrayList<FactoryType> aft = loadSockets();
            socketTypes = new FactoryType[aft.size()];
            for (int i = 0; i < socketTypes.length; i++) {
                socketTypes[i] = aft.get(i);
            }
        }
        return socketTypes;
    }

    @Override
    public String getUnlocalizedName(ItemStack is) {
        int md = is.getItemDamage();
        String ret = getUnlocalizedName() + FactoryType.fromMd((byte) md);
        return ret;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item itemId, CreativeTabs tab, List list) {
        FactoryType[] ss = getSockets();
        for (int i = 0; i < ss.length; i++) {
            FactoryType ft = ss[i];
            list.add(ft.asSocketItem());
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        return true;
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        list.add(LangUtil.translate("item.factorization:socket_info"));
    }
}
