package factorization.common;

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
        Core.instance.addName(this, "Charge Meter");
        setIconIndex(6);
        setTextureFile(Core.texture_file_item);
    }

    @Override
    public boolean onItemUse(ItemStack is, EntityPlayer player,
            World w, int x, int y, int z, int side) {
        Coord here = new Coord(w, x, y, z);
        IChargeConductor ic = here.getTE(IChargeConductor.class);
        if (ic == null) {
            return false;
        }
        if (!Core.instance.isCannonical(w)) {
            return true;
        }
        int toLook = 5;
        if (player.isSneaking()) {
            toLook = 10;
        }
        ChargeDensityReading ret = Charge.getChargeDensity(ic, toLook);
        float density = ret.totalCharge / ((float) ret.conductorCount);
        String d = String.format("%.1f", density);
        //TODO: Let's put it somewhere better than the chat log
        player.addChatMessage("Average: " + d
                + "  Target: " + ic.getCharge().getValue()
                + "  Conductors: " + ret.conductorCount
                //+ "  Total: " + ret.totalCharge
                );
        return true;
    }

}
