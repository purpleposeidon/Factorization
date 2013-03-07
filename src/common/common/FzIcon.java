package factorization.common;

import java.util.ArrayList;
import java.util.LinkedList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.client.renderer.texture.TextureStitched;
import net.minecraft.util.Icon;

@SideOnly(Side.CLIENT)
public class FzIcon implements Icon {
    static LinkedList<FzIcon> unregistered = new LinkedList<FzIcon>();
    static boolean ran = false;
    
    String name;
    Icon under;
    
    public FzIcon(String filename) {
        this.name = "factorization/" + filename;
        if (ran) {
            Core.logSevere("Registering icon " + filename);
            if (Core.dev_environ) {
                throw new IllegalArgumentException("Late icon registration");
            }
        }
        unregistered.add(this);
    }

    static void registerNew(IconRegister reg) {
        while (unregistered.size() > 0) {
            FzIcon fz = unregistered.remove();
            fz.under = Core.texture(reg, fz.name);
        }
    }
    
    static void lock() {
        ran = true;
    }

    @Override
    public int getOffsetX() {
        return under.getOffsetX();
    }

    @Override
    public int getOffsetY() {
        return under.getOffsetY();
    }

    @Override
    public float getU1() {
        return under.getU1();
    }

    @Override
    public float getU2() {
        return under.getU2();
    }

    @Override
    public float interpolateU(double d0) {
        return under.interpolateU(d0);
    }

    @Override
    public float getV1() {
        return under.getV1();
    }

    @Override
    public float getV2() {
        return under.getV2();
    }

    @Override
    public float interpolateV(double d0) {
        return under.interpolateV(d0);
    }

    @Override
    public String getName() {
        return under.getName();
    }

    @Override
    public int getWidth() {
        return under.getWidth();
    }

    @Override
    public int getHeight() {
        return getHeight();
    }
    
    //java = teh best0rxxz!
}
