package factorization.shared;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

public class FactorizationRenderNonTE implements ISimpleBlockRenderingHandler {
    public FactorizationRenderNonTE() {
        Core.nonte_rendertype = RenderingRegistry.getNextAvailableRenderId();
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        if (block instanceof IRenderNonTE) {
            FactorizationBlockRender FBR = ((IRenderNonTE) block).getFBR();
            FBR.renderInInventory();
            FBR.setMetadata(metadata);
            if (FBR.renderType == IItemRenderer.ItemRenderType.EQUIPPED
                    || FBR.renderType == IItemRenderer.ItemRenderType.EQUIPPED_FIRST_PERSON) {
                GL11.glPushAttrib(GL11.GL_DEPTH_BUFFER_BIT);
                GL11.glDepthMask(true);
                FBR.render(renderer);
                GL11.glPopAttrib();
            } else {
                FBR.render(renderer);
            }
        }
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z,
                                    Block block, int modelId, RenderBlocks renderBlocks) {
        int renderPass = 0; //MinecraftForgeClient.getRenderPass(); // Ohhh, look, that helpful method's returned! Like 2 years after I asked for it back! Except now it's broken. Great job, Forge.
        if (block instanceof IRenderNonTE) {
            int fmd = world.getBlockMetadata(x, y, z);
            FactorizationBlockRender FBR = ((IRenderNonTE) block).getFBR();
            FBR.renderInWorld(world, x, y, z, fmd, null);
            boolean ret = false;
            if (renderPass == 0) {
                ret = FBR.render(renderBlocks);
            } else if (renderPass == 1) {
                ret = FBR.renderSecondPass(renderBlocks);
            }
            FBR.clearWorldReferences();
            return ret;
        }
        return false;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return Core.nonte_rendertype;
    }
}
