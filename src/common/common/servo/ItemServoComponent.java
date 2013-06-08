package factorization.common.servo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.common.Core.TabType;

public class ItemServoComponent extends Item {
    public ItemServoComponent(int itemId) {
        super(itemId);
        setUnlocalizedName("factorization:servo/component");
        Core.tab(this, TabType.SERVOS);
    }
    
    public String getUnlocalizedName(ItemStack is) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return super.getUnlocalizedName();
        }
        return super.getUnlocalizedName(is) + "." + sc.getName();
    }
    
    public String getItemDisplayName(ItemStack is) {
        String s = super.getItemDisplayName(is);
        if (s == null || s.length() == 0) {
            s = getUnlocalizedName(is);
            System.out.println(s); //NORELEASE
        }
        return s;
    };
    
    ServoComponent get(ItemStack is) {
        if (!is.hasTagCompound()) {
            return null;
        }
        return ServoComponent.load(is.getTagCompound());
    }
    
    void update(ItemStack is, ServoComponent sc) {
        if (sc != null) {
            sc.save(FactorizationUtil.getTag(is));
        } else {
            is.setTagCompound(null);
        }
    }
    
    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return is;
        }
        if (sc instanceof Actuator) {
            Actuator ac = (Actuator) sc;
            ac.onUse(player, player.isSneaking());
        } else {
            return is;
        }
        update(is, sc);
        return is;
    }
    
    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world, int x, int y, int z, int side, float vx, float vy, float vz) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return false;
        }
        if (sc instanceof Decorator) {
            Decorator dec = (Decorator) sc;
            Coord here = new Coord(world, x, y, z);
            TileEntityServoRail rail = here.getTE(TileEntityServoRail.class);
            if (rail != null && rail.decoration == null) {
                rail.setDecoration(dec);
            }
            if (world.isRemote){
                here.redraw();
            }
        }
        return super.onItemUse(is, player, world, x, y, z, side, vx, vy, vz);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        super.addInformation(is, player, list, verbose);
        ServoComponent sc = get(is);
        if (sc != null) {
            sc.addInformation(list);
        }
        Core.brand(is, list);
    }
    
    
    private List<ItemStack> subItemsCache = null;
    
    void loadSubItems() {
        if (subItemsCache != null) {
            return;
        }
        subItemsCache = new ArrayList<ItemStack>(100);
        ArrayList<Object> temp = new ArrayList();
        for (Class<? extends ServoComponent> scClass : ServoComponent.getComponents()) {
            try {
                ServoComponent sc = scClass.newInstance();
                subItemsCache.add(sc.toItem());
            } catch (Throwable e) {
                e.printStackTrace();
                continue;
            }
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(int id, CreativeTabs tab, List list) {
        loadSubItems();
        list.addAll(subItemsCache);
    }
    
    @Override
    public Icon getIcon(ItemStack stack, int pass) {
        return super.getIcon(stack, pass);
    }
}
