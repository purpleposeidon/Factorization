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
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.RenderingCube.Vector;
import factorization.common.TileEntityGreenware;

public class BlockRenderSculpture extends FactorizationBlockRender {
    static BlockRenderSculpture instance;
    
    public BlockRenderSculpture() {
        instance = this;
        cubeTexture = Core.texture_file_ceramics;
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
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
        }
        renderStand();
//		if (world_mode) {
//			TileEntityGreenware teg = getCoord().getTE(TileEntityGreenware.class);
//			if (teg != null) {
//				renderStatic(teg);
//			}
//		}
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        for (RenderingCube rc : greenware.parts) {
            if (rc == greenware.selected) {
                rc.setIcon(greenware.selectedIcon); //portal
                //rc.setIcon(0); //error texture
                //rc.setIcon(16*16 - 1); //lava
                //rc.setIcon(2*16 - 1); //fire
                if (rc.theta != 0) {
                    glDisable(GL_TEXTURE_2D);
                    glPushMatrix();
                    glTranslatef(0.5F, 0.5F, 0.5F);
                    glColor4f(abs(rc.axis.x), 1, 1, 1);
                    
                    glBegin(GL_LINES);
                    glVertex3f(rc.axis.x, rc.axis.y, rc.axis.z);
                    glVertex3f(-rc.axis.x, -rc.axis.y, -rc.axis.z);
                    glEnd();
                    
                    glPopMatrix();
                    glEnable(GL_TEXTURE_2D);
                }
            } else {
                rc.setIcon(greenware.clayIcon);
            }
            renderClayCube(rc);
        }
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        //create a display list! Or something.
        //well, create a tessellator actually!
        //Or something. What we need to do is...
        //make an array of tessellator calls. Then when this happens, we just run through that array!
        //Or possibly not do that, and instead just use the TE...
        for (RenderingCube rc : greenware.parts) {
            renderClayCube(rc);
        }
    }
    
    RenderingCube invWoodStand = new RenderingCube(2, new Vector(4, 1, 4), new Vector(0, -6, 0));
    RenderingCube worldWoodStand = new RenderingCube(16 + 8, new Vector(4, 1, 4), new Vector(0, -6, 0));
    
    void renderStand() {
        if (world_mode) {
            renderClayCube(worldWoodStand);
        } else {
            renderClayCube(invWoodStand);
        }
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.GREENWARE;
    }

}
