package factorization.sockets;

import factorization.api.annotation.Nonnull;
import factorization.common.FactoryType;
import factorization.shared.Core.TabType;
import factorization.shared.ISensitiveMesh;
import factorization.shared.ItemFactorization;
import factorization.util.ItemUtil;
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

/**
 * This class is sort-of-deprecated. Some sockets require multiple items
 * to be constructed, and it'd be reasonable to construct sockets multiple ways
 * (using, say, different motors from different mods, rendering with different models)
 * This is being kept just for the sake of world compatibility;
 * it'd be better to have separate items for each part.
 */
public class ItemSocketPart extends ItemFactorization implements ISensitiveMesh {

    public ItemSocketPart(String name, TabType tabType) {
        super(name, tabType);
        setHasSubtypes(true);
        setMaxDamage(0);
    }
    
    
    ArrayList<FactoryType> loadSockets() {
        ArrayList<FactoryType> ret = new ArrayList<FactoryType>();
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
        return getUnlocalizedName() + FactoryType.fromMd((byte) md);
    }
    
    @Override
    public void getSubItems(Item itemId, CreativeTabs tab, List<ItemStack> list) {
        FactoryType[] ss = getSockets();
        for (FactoryType ft : ss) {
            list.add(ft.asSocketItem());
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ) {
        return true;
    }

    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List<String> list, boolean verbose) {
        list.add(LangUtil.translate("item.factorization:socket_info"));
    }

    @Override
    public String getMeshName(@Nonnull ItemStack is) {
        int md = is.getItemDamage();
        FactoryType ft = FactoryType.fromMd((short) md);
        if (ft == null) return "invalid";
        return "socket/" + ft.name().toLowerCase();
    }

    @Nonnull
    @Override
    public List<ItemStack> getMeshSamples() {
        return ItemUtil.getSubItems(this);
    }
}
