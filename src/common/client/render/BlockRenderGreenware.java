package factorization.client.render;

import net.minecraft.client.renderer.RenderBlocks;
import factorization.common.FactoryType;
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
    }
    
    @Override
    void render(RenderBlocks rb) {
        /*
        if (!world_mode) {
            renderStand();
            renderGenericLump();
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
        */
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        /*for (RenderingCube rc : greenware.parts) {
            rc.setIcon(greenware.getIcon(rc));
            renderCube(rc);
        }*/
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        //create a display list! Or something.
        //well, create a tessellator actually!
        //Or something. What we need to do is...
        //make an array of tessellator calls. Then when this happens, we just run through that array!
        //Or possibly not do that, and instead just use the TE...
        /*for (RenderingCube rc : greenware.parts) {
            rc.setIcon(greenware.getIcon(rc));
            renderCube(rc);
        }*/
    }
    
    /*
    static RenderingCube woodStand = new RenderingCube(16*12 + 2, new VectorUV(4, 1, 4));
    static RenderingCube genericLump = new RenderingCube(16*12, new VectorUV(3, 5, 3));
    static {
        woodStand.trans.translate(0, -6, 0);
    }
    
    void renderStand() {
        renderCube(woodStand);
    }
    
    void renderGenericLump() {
        renderCube(genericLump);
    }
    */

    @Override
    FactoryType getFactoryType() {
        return FactoryType.GREENWARE;
    }

}
