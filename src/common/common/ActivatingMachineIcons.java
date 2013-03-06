package factorization.common;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ActivatingMachineIcons {
    FzIcon top, bottom, sideOn, sideOff;
    
    public ActivatingMachineIcons(String name) {
        top = new FzIcon(name + "_top");
        bottom = new FzIcon(name + "_bottom");
        sideOff = new FzIcon(name + "_side");
        sideOn = new FzIcon(name + "_side_on");
    }
    
    FzIcon get(TileEntityFactorization tef, ForgeDirection side) {
        switch (side) {
        case UP: return top;
        case DOWN: return bottom;
        default:
            if (tef.draw_active > 0) {
                return sideOn;
            }
            return sideOff;
        }
    }
}
