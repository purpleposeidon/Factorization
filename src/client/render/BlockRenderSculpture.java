package factorization.client.render;

import cpw.mods.fml.client.SpriteHelper;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
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
        cubeTexture = "/terrain.png";
    }
    
    private boolean texture_init = false;
    void setup() {
        if (texture_init) {
            return;
        }
        texture_init = true;
        String free =
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111" +
                "1111111111111111";
        SpriteHelper.registerSpriteMapForFile(Core.texture_file_ceramics, free);
        //RenderingRegistry.addTextureOverride(Core.texture_file_ceramics, cubeTexture, TileEntityGreenware.clayIcon_src);
        for (int i = 0; i < 16*16; i++) {
            RenderingRegistry.addTextureOverride(cubeTexture, Core.texture_file_ceramics, i);
            //RenderingRegistry.addTextureOverride(Core.texture_file_ceramics, cubeTexture, i);
        }
    }
    
    @Override
    void render(RenderBlocks rb) {
        setup();
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
        setup();
        for (RenderingCube rc : greenware.parts) {
            if (rc == greenware.selected) {
                rc.setIcon(greenware.selectedIcon); //portal
                //rc.setIcon(0); //error texture
                //rc.setIcon(16*16 - 1); //lava
                //rc.setIcon(2*16 - 1); //fire
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
    
    RenderingCube invWoodStand = new RenderingCube(4, new Vector(4, 1, 4), new Vector(0, -6, 0));
    RenderingCube worldWoodStand = new RenderingCube(16 + 8, new Vector(4, 1, 4), new Vector(0, -6, 0));
    
    void renderStand() {
        worldWoodStand = new RenderingCube(16 + 8, new Vector(7, 0.5F, 7), new Vector(0, -7.5F, 0));
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
