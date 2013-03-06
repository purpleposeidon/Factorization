package factorization.common;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SimpleMachineIcons {
    FzIcon top, side, face;
    
    public SimpleMachineIcons(String name) {
        top = new FzIcon(name + "_top");
        side = new FzIcon(name + "_side");
        face = new FzIcon(name + "_face");
    }
    
    Icon get(ForgeDirection teFace, ForgeDirection renderSide) {
        if (teFace == renderSide) {
            return face;
        }
        if (renderSide.offsetY == 0) {
            return top;
        }
        return side;
    }
}
