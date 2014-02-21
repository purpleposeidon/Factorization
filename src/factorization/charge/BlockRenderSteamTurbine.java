package factorization.charge;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderSteamTurbine extends FactorizationBlockRender {

    TileEntityWire fake_wire = new TileEntityWire();
    @Override
    public void render(RenderBlocks rb) {
        float m = 0.0001F;
        renderNormalBlock(rb, getFactoryType().md);
        
        renderMotor(rb, 0);
        if (world_mode) {
            Coord me = getCoord();
            fake_wire.setWorldObj(me.w);
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
        
        if (world_mode) {
            //render interior bits
            Block b = Core.registry.factory_rendering_block;
            float f = 1F - (3F/16F);
            
            IIcon side = BlockIcons.turbine_side;
            //NORELEASE: this is ass; de-ass
            Tessellator.instance.addTranslation(0, 0, f);
            rb.renderFaceZNeg(b, x, y, z, side);
            Tessellator.instance.addTranslation(0, 0, -2*f);
            rb.renderFaceZPos(b, x, y, z, side);
            Tessellator.instance.addTranslation(0, 0, f);
            
            Tessellator.instance.addTranslation(f, 0, 0);
            rb.renderFaceXNeg(b, x, y, z, side);
            Tessellator.instance.addTranslation(-2*f, 0, 0);
            rb.renderFaceXPos(b, x, y, z, side);
            Tessellator.instance.addTranslation(f, 0, 0);
            
            Tessellator.instance.addTranslation(0, f, 0);
            rb.renderFaceYNeg(b, x, y, z, BlockIcons.turbine_bottom);
            Tessellator.instance.addTranslation(0, -2*f, 0);
            rb.renderFaceYPos(b, x, y, z, BlockIcons.turbine_bottom);
            Tessellator.instance.addTranslation(0, f, 0);
        } else {
            //render fan
            GL11.glPushMatrix();
            float s = 0.60F;
            GL11.glScalef(s, s, s);
            GL11.glTranslatef(-0.5F, 0.1F, -0.5F);
            GL11.glRotatef(90, 1, 0, 0);
            renderItemIIcon(Core.registry.fan.getIconFromDamage(0));
            GL11.glPopMatrix();
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STEAMTURBINE;
    }

}
