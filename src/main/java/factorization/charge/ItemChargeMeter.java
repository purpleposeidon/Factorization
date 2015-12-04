package factorization.charge;

import factorization.api.Charge;
import factorization.api.Charge.ChargeDensityReading;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import factorization.api.IMeterInfo;
import factorization.notify.Notice;
import factorization.notify.NoticeUpdater;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.shared.ItemFactorization;
import factorization.util.PlayerUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ItemChargeMeter extends ItemFactorization {

    public ItemChargeMeter() {
        super("tool/charge_meter", TabType.TOOLS);
        setMaxStackSize(1);
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
        if (w.isRemote) {
            return true;
        }
        Coord here = new Coord(w, x, y, z);
        final TileEntity te = here.getTE();
        if (te == null) return false;
        final IChargeConductor ic = here.getTE(IChargeConductor.class);
        player = PlayerUtil.fakeplayerToNull(player);
        if (ic == null) {
            final IMeterInfo im = here.getTE(IMeterInfo.class);
            if (im == null) {
                return false;
            }
            new Notice(te, new NoticeUpdater() {
                @Override
                public void update(Notice msg) {
                    String info = im.getInfo();
                    if (info == null) return;
                    msg.setMessage("%s", info);
                }
            }).send(player);
            return true;
        }
        new Notice(te, new NoticeUpdater() {
            @Override
            public void update(Notice msg) {
                ChargeDensityReading ret = Charge.getChargeDensity(ic);
                String inf = ic.getInfo();
                if (inf == null || inf.length() == 0) {
                    inf = "";
                } else {
                    inf = "\n" + inf;
                }
                String txt;
                if (Core.dev_environ) { 
                    txt = "Charge: " + ic.getCharge().getValue() + "/" + ret.totalCharge
                        + "\nConductors: " + ret.conductorCount;
                } else {
                    txt = "Charge: " + ic.getCharge().getValue();
                }
                txt += inf;
                msg.setMessage("%s", txt);
            }
        }).send(player);
        return true;
    }
}
