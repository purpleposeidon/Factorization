package factorization.weird;

import java.util.List;

import factorization.util.LangUtil;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemBlockProxy;
import factorization.weird.TileEntityDayBarrel.Type;

public class ItemDayBarrel extends ItemBlockProxy {

    public ItemDayBarrel(String name) {
        super(Core.registry.factory_block_barrel, Core.registry.daybarrel_item_hidden, name, TabType.BLOCKS);
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
    public String getItemStackDisplayName(ItemStack is) {
        Type upgrade = TileEntityDayBarrel.getUpgrade(is);
        String lookup = "factorization.factoryBlock.DAYBARREL.format";
        if (upgrade != Type.NORMAL) {
            lookup = "factorization.factoryBlock.DAYBARREL.format2";
        }
        String type = LangUtil.translate("factorization.factoryBlock.DAYBARREL." + upgrade);
        return LangUtil.translateWithCorrectableFormat(lookup, type, TileEntityDayBarrel.getLog(is).getDisplayName());
    }
    
    @Override
    @SideOnly(Side.CLIENT) // Invokes a client-only function getTooltip
    protected void addExtraInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Type upgrade = TileEntityDayBarrel.getUpgrade(is);
        if (upgrade == Type.SILKY) {
            list.add(LangUtil.translateThis("factorization.factoryBlock.DAYBARREL.SILKY.silkhint"));
            TileEntityDayBarrel db = (TileEntityDayBarrel) FactoryType.DAYBARREL.getRepresentative();
            db.loadFromStack(is);
            int count = db.getItemCount();
            if (count > 0 && db.item != null) {
                if (db.item.getItem() == this) {
                    list.add("?");
                    return;
                }
                List sub = db.item.getTooltip/* Client-only */(player, false /* Propagating verbose would be natural, but let's keep the tool-tip short */);
                db.item.getItem().addInformation(db.item, player, sub, verbose);
                if (!sub.isEmpty()) {
                    Object first = sub.get(0);
                    sub.set(0, count + " " + first);
                    list.addAll(sub);
                }
            }
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IIconRegister) { }
}
