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

public class ItemChargeMeter extends Item {

    protected ItemChargeMeter(int par1) {
        super(par1);
        setItemName("factorization.chargemeter");
        Core.proxy.addName(this, "Charge Meter");
        setIconIndex(6);
        setTextureFile(Core.texture_file_item);
        setTabToDisplayOn(CreativeTabs.tabRedstone);
    }
    
    

    @Override
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
        if (inf == null) {
            inf = "";
        } else {
            inf = "  " + inf;
        }
        player.addChatMessage("Target: " + ic.getCharge().getValue()
                + "  Average: " + d
                + "  Total: " + ret.totalCharge
                + "  Conductors: " + ret.conductorCount
                + inf
                //+ "  C: " + ic.getCoord()
                //+ "  Total: " + ret.totalCharge
                );
        return true;
    }

    @Override
    public void addInformation(ItemStack is, List infoList) {
        super.addInformation(is, infoList);
        Core.brand(infoList);
    }
}
