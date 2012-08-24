package factorization.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.src.RenderBlocks;
import net.minecraft.src.TexturedQuad;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.Texture;

public class BlockRenderGrinder extends FactorizationBlockRender {
    @Override
    void render(RenderBlocks rb) {
        renderMotor(rb, 8F/16F);
        float p = 1F/16F;
        float p2 = 2*p, p3 = 3*p;
        int dark_iron = 3+16*2, metal = 14, lead = 2+16*2;;
        //bottom plate
        renderPart(rb, dark_iron, 2*p, 0, 2*p, 1-2*p, 2*p, 1-2*p);
        //top cap
        renderPart(rb, metal, 0, 1-p3, 0, 1, 1-p, 1);
        //side edges
        renderPart(rb, metal, 0, 0, 0, p2, 1-p3, p2);
        renderPart(rb, metal, 1-p2, 0, 1-p2, 1, 1-p3, 1);
        renderPart(rb, metal, 0, 0, 1-p2, p2, 1-p3, 1);
        renderPart(rb, metal, 1-p2, 0, 0, 1, 1-p3, p2);
        //bottom edges
        renderPart(rb, lead, 1-p2, 0, p2, p2, p*4, 0);
        renderPart(rb, lead, 1-p2, 0, 1, p2, p*4, 1-p2);
        renderPart(rb, lead, 0, 0, p2, p2, p*4, 1-p2);
        renderPart(rb, lead, 1-p2, 0, p2, 1, p*4, 1-p2);
        
        if (!world_mode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(0, -0.5F+5F/16F, 0);
            GL11.glRotatef(45/2, 0, 1, 0);
            TileEntityGrinderRender.renderGrindHead();
            GL11.glPopMatrix();
        }
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.GRINDER;
    }

}
