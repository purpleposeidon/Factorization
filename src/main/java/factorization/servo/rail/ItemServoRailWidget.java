package factorization.servo.rail;

import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.util.ItemUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class ItemServoRailWidget extends ItemFactorization {
    public ItemServoRailWidget(String name) {
        super(name, TabType.SERVOS);
    }
    
    @Override
    public String getUnlocalizedName(ItemStack is) {
        ServoComponent sc = get(is);
        if (sc == null) {
            return super.getUnlocalizedName();
        }
        return super.getUnlocalizedName(is) + "." + sc.getName();
    }
    
    @Override
    public String getItemStackDisplayName(ItemStack is) {
        String s = super.getItemStackDisplayName(is);
        if (s == null || s.length() == 0) {
            s = getUnlocalizedName(is);
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
            sc.save(ItemUtil.getTag(is));
        } else {
            is.setTagCompound(null);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        ServoComponent sc = get(is);
        if (sc != null) {
            sc.addInformation(list);
        }
    }


    private List<ItemStack> subItemsCache = null;
    
    void loadSubItems() {
        if (subItemsCache != null) {
            return;
        }
        if (this == Core.registry.servo_widget_instruction) {
            subItemsCache = ServoComponent.sorted_instructions;
        } else if (this == Core.registry.servo_widget_decor) {
            subItemsCache = ServoComponent.sorted_decors;
        } else {
            subItemsCache = new ArrayList<ItemStack>();
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item id, CreativeTabs tab, List<ItemStack> list) {
        loadSubItems();
        list.addAll(subItemsCache);
    }
}
