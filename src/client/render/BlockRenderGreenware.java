package factorization.client.render;

import static org.lwjgl.opengl.GL11.*;
import static java.lang.Math.abs;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.SpriteHelper;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraftforge.client.ForgeHooksClient;
import factorization.api.Coord;
import factorization.api.VectorUV;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.TileEntityGreenware.ClayState;
import factorization.common.TileEntityGreenware;

public class BlockRenderGreenware extends FactorizationBlockRender {
    static BlockRenderGreenware instance;
    
    public BlockRenderGreenware() {
        instance = this;
        setup();
    }
    
    private boolean texture_init = false;
    public void setup() {
        if (texture_init) {
            return;
        }
        //TODO: Copy some textures from terrain.png over here
    }
    
    @Override
    void render(RenderBlocks rb) {
        if (!world_mode) {
            renderStand();
            return;
        }
        TileEntityGreenware gw = getCoord().getTE(TileEntityGreenware.class);
        if (gw == null) {
            return;
        }
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
        }
        ClayState state = gw.getState();
        if (state == ClayState.DRY || state == ClayState.WET) {
            renderStand();
        }
        if (!gw.canEdit()) {
            renderStatic(gw);
        }
        gw.renderedAsBlock = true;
//		if (world_mode) {
//			TileEntityGreenware teg = getCoord().getTE(TileEntityGreenware.class);
//			if (teg != null) {
//				renderStatic(teg);
//			}
//		}
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        for (RenderingCube rc : greenware.parts) {
//			if (greenware.isSelected(rc) && rc.theta != 0) {
//				glDisable(GL_TEXTURE_2D);
//				glPushMatrix();
//				glTranslatef(0.5F, 0.5F, 0.5F);
//				glColor4f(abs(rc.axis.x), abs(rc.axis.y), abs(rc.axis.z), 1);
//				
//				glBegin(GL_LINES);
//				glVertex3f(rc.axis.x, rc.axis.y, rc.axis.z);
//				glVertex3f(-rc.axis.x, -rc.axis.y, -rc.axis.z);
//				glEnd();
//				
//				glPopMatrix();
//				glEnable(GL_TEXTURE_2D);
//			}
            rc.setIcon(greenware.getIcon(rc));
            renderCube(rc);
        }
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        //create a display list! Or something.
        //well, create a tessellator actually!
        //Or something. What we need to do is...
        //make an array of tessellator calls. Then when this happens, we just run through that array!
        //Or possibly not do that, and instead just use the TE...
        for (RenderingCube rc : greenware.parts) {
            rc.setIcon(greenware.getIcon(rc));
            renderCube(rc);
        }
    }
    
    static RenderingCube woodStand = new RenderingCube(16*12 + 2, new VectorUV(4, 1, 4));
    static {
        woodStand.trans.translate(0, -6, 0);
    }
    
    void renderStand() {
        renderCube(woodStand);
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.GREENWARE;
    }

}
