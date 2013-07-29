package factorization.common;

import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.Core.TabType;
import factorization.common.TileEntityDayBarrel.Type;

public class ItemDayBarrel extends ItemBlockProxy {

    protected ItemDayBarrel(int id, String name) {
        super(id, Core.registry.daybarrel_item_hidden, name, TabType.BLOCKS);
        setMaxDamage(0);
        setNoRepair();
    }
    
    @Override
    public boolean getShareTag() {
        return true;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return 0;
    }
    
    @Override
    public String getItemDisplayName(ItemStack is) {
        Type upgrade = TileEntityDayBarrel.getUpgrade(is);
        String lookup = "factorization.factoryBlock.DAYBARREL.format";
        if (upgrade != Type.NORMAL) {
            lookup = "factorization.factoryBlock.DAYBARREL.format2";
        }
        lookup = Core.translate(lookup);
        return String.format(lookup, upgrade.toString(), TileEntityDayBarrel.getLog(is).getDisplayName());
    }
}
