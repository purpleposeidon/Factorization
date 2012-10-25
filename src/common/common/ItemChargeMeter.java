package factorization.common;

import java.util.List;

import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import factorization.api.Charge;
import factorization.api.Charge.ChargeDensityReading;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.common.Core.TabType;

public class ItemChargeMeter extends Item {

    protected ItemChargeMeter(int par1) {
        super(par1);
        setItemName("factorization.chargemeter");
        Core.proxy.addName(this, "Charge Meter");
        setIconIndex(6);
        setTextureFile(Core.texture_file_item);
        Core.tab(this, TabType.REDSTONE);
    }
    
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
    }

    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y,
            int z, int side, float vecx, float vecy, float vecz) {
        Coord here = new Coord(w, x, y, z);
        IChargeConductor ic = here.getTE(IChargeConductor.class);
        if (ic == null) {
            return false;
        }
        if (w.isRemote) {
            return true;
        }
        ChargeDensityReading ret = Charge.getChargeDensity(ic);
        float density = ret.totalCharge / ((float) ret.conductorCount);
        String d = String.format("%.1f", density);
        //TODO: Let's put it somewhere better than the chat log
        String inf = ic.getInfo();
        if (inf == null || inf.length() == 0) {
            inf = "";
        } else {
            inf = "\n" + inf;
        }
        /*
         * targetCharge/totalCharge
         * Conductors:
         */
        Core.notify(player, here,
                "Charge: " + ic.getCharge().getValue() + "/" + ret.totalCharge
                + "\nConductors: " + ret.conductorCount
                + inf
                //+ "  C: " + ic.getCoord()
                //+ "  Total: " + ret.totalCharge
                );
        return true;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List list, boolean verbose) {
        Core.brand(list);
    }
}
