package factorization.client.render;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.Texture;
import factorization.common.TileEntitySolarTurbine;
import factorization.common.TileEntityWire;
import factorization.common.WireConnections;
import factorization.common.WireRenderingCube;
import net.minecraft.src.RenderBlocks;
import net.minecraftforge.client.MinecraftForgeClient;

public class BlockRenderSolarTurbine extends FactorizationBlockRender {

    @Override
    void render(RenderBlocks rb) {
        if (world_mode) {
            Coord me = getCoord();
            TileEntitySolarTurbine st = me.getTE(TileEntitySolarTurbine.class);
            if (st == null) {
                return;
            }
            renderSolarTurbine(rb, st.water_level, me);
        } else {
            renderSolarTurbine(rb, 0, null);
        }
    }
    
    static final float d = 1F / 16F;
    
    @Override
    void renderSecondPass(RenderBlocks rb) {
        Coord me = getCoord();
        TileEntitySolarTurbine st = me.getTE(TileEntitySolarTurbine.class);
        if (st == null) {
            return;
        }
        if (st.water_level <= 1F / 16F) {
            return;
        }
        int water_height = st.water_level;
        //Tessellator.instance.setColorOpaque_F(0.5F, 0.5F, 0.5F);
        //Tessellator.instance.setColorOpaque(255, 0, 255);
        renderPart(rb, 7, d, 0.001F, d, 1 - d, (0.95F + water_height / (TileEntitySolarTurbine.max_water / 4)) / 16F, 1 - d);
        //			renderPart(rb, glass, 1 - d, 1 - d, 1 - d, d, 0.02F, d);
        return;
    }
    
    private TileEntityWire fake_wire = new TileEntityWire();
    void renderSolarTurbine(RenderBlocks rb, int water_height, Coord me) {
        int glass = Texture.lamp_iron + 10;
        int water = 7;
        float m = 0.0001F;
        renderPart(rb, glass, 0 + m, 0 + m, 0 + m, 1 - m, 1 - m, 1 - m);
        if (!world_mode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-0.5F, 0.1F, -0.5F);
            GL11.glRotatef(90, 1, 0, 0);
            renderItemIn2D(10);
            GL11.glPopMatrix();
        }
        renderMotor(rb, 0);
        if (world_mode) {
            fake_wire.worldObj = me.w;
            fake_wire.xCoord = me.x;
            fake_wire.yCoord = me.y;
            fake_wire.zCoord = me.z;
            fake_wire.supporting_side = 0;
            WireConnections con = new WireConnections(fake_wire);
            con.conductorRestrict();
            for (WireRenderingCube rc : con.getParts()) {
                renderCube(rc);
            }
        }
    }
    
    @Override
    FactoryType getFactoryType() {
        return FactoryType.SOLARTURBINE;
    }

}
