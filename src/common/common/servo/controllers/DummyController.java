package factorization.common.servo.controllers;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.renderer.RenderBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.common.servo.Controller;
import factorization.common.servo.ServoMotor;
import factorization.common.servo.ServoStack;

public class DummyController extends Controller {

    @Override
    public void doUpdate(ServoMotor motor) { }

    @Override
    public String getName() {
        return "fz.controller.dummyDefault";
    }

    @Override
    protected void putData(DataHelper data) throws IOException { }

    @Override
    public boolean configure(ServoStack stack) {
        return false;
    }

    @Override
    public void deconfigure(List<Object> stack) { }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) { }

    
}
