package factorization.common.servo.controllers;

import java.io.IOException;

import net.minecraft.client.renderer.RenderBlocks;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.servo.Controller;
import factorization.common.servo.ServoMotor;

public class DummyController extends Controller {

    @Override
    public void doUpdate(ServoMotor motor) { }

    @Override
    public String getName() {
        return "fz.controller.dummyDefault";
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException { return this; }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) { }

    
}
