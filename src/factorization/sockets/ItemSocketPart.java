package factorization.sockets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.FactoryType;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;

@Deprecated
public class ItemSocketPart extends ItemFactorization {

    public ItemSocketPart(int itemId, String name, TabType tabType) {
        super(itemId, name, tabType);
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
    
    @SideOnly(Side.CLIENT)
    IIcon[] socketIIcons;

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIIcons(IIconRegister register) {
        socketIIcons = new IIcon[FactoryType.MAX_ID];
        ItemStack me = new ItemStack(this);
        for (FactoryType ft : getSockets()) {
            me.setItemDamage(ft.md);
            socketIIcons[ft.md] = register.registerIIcon(getUnlocalizedName(me).replace("item.", ""));
        }
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        int md = is.getItemDamage();
        String ret = getUnlocalizedName() + FactoryType.fromMd(md);
        return ret;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(int itemId, CreativeTabs tab, List list) {
        FactoryType[] ss = getSockets();
        for (int i = 0; i < ss.length; i++) {
            FactoryType ft = ss[i];
            list.add(ft.asSocketItem());
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIIconFromDamage(int md) {
        if (md > 0 && md < socketIIcons.length) {
            return socketIIcons[md];
        }
        return super.getIIconFromDamage(md);
    }
    
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player,
            World world, int x, int y, int z, int side,
            float hitX, float hitY, float hitZ) {
        int md = is.getItemDamage();
        Coord here = new Coord(world, x, y, z);
        is.stackSize--;
        SocketEmpty se = here.getTE(SocketEmpty.class);
        if (se == null) {
            return super.onItemUse(is, player, world, x, y, z, side, hitX, hitY, hitZ);
        }
        if (md > 0 && md < FactoryType.MAX_ID) {
            try {
                TileEntitySocketBase socket = (TileEntitySocketBase) FactoryType.fromMd(md).getFactoryTypeClass().newInstance();
                here.setTE(socket);
                socket.facing = se.facing;
                here.markBlockForUpdate();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return true;
    }
    
    @Override
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        list.add("Socket part");
    }
}
