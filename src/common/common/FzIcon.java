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
    public int func_94211_a() {
        return under.func_94211_a();
    }

    @Override
    public int func_94216_b() {
        return under.func_94216_b();
    }

    @Override
    public float func_94209_e() {
        return under.func_94209_e();
    }

    @Override
    public float func_94212_f() {
        return under.func_94212_f();
    }

    @Override
    public float func_94214_a(double d0) {
        return under.func_94214_a(d0);
    }

    @Override
    public float func_94206_g() {
        return under.func_94206_g();
    }

    @Override
    public float func_94210_h() {
        return under.func_94210_h();
    }

    @Override
    public float func_94207_b(double d0) {
        return under.func_94207_b(d0);
    }

    @Override
    public String func_94215_i() {
        return under.func_94215_i();
    }

    @Override
    public int func_94213_j() {
        return under.func_94213_j();
    }

    @Override
    public int func_94208_k() {
        return func_94208_k();
    }
    
    //java = teh best0rxxz!
}
