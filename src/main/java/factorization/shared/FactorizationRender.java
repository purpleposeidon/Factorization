package factorization.shared;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.client.registry.RenderingRegistry;
import factorization.wrath.BlockLightAir;

public class FactorizationRender implements ISimpleBlockRenderingHandler {
    public FactorizationRender() {
        Core.factory_rendertype = RenderingRegistry.getNextAvailableRenderId();
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        if (block == Core.registry.factory_block || block == Core.registry.factory_block_barrel) {
            FactorizationBlockRender FBR = FactorizationBlockRender.getRenderer(metadata);
            FBR.renderInInventory();
            FBR.setMetadata(metadata);
            if (FBR.renderType == ItemRenderType.EQUIPPED
                    || FBR.renderType == ItemRenderType.EQUIPPED_FIRST_PERSON) {
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
        Core.profileStart("fz");
        try {
            int md = world.getBlockMetadata(x, y, z);
            int renderPass = BlockFactorization.CURRENT_PASS; //MinecraftForgeClient.getRenderPass(); //Bluh
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityCommon) {
                TileEntityCommon tec = (TileEntityCommon) te;
                int fmd = tec.getFactoryType().md;
                FactorizationBlockRender FBR = FactorizationBlockRender.getRenderer(fmd);
                FBR.renderInWorld(world, x, y, z, fmd, tec);
                boolean ret = false;
                if (renderPass == 0) {
                    ret = FBR.render(renderBlocks);
                } else if (renderPass == 1) {
                    ret = FBR.renderSecondPass(renderBlocks);
                }
                FBR.clearWorldReferences();
                return ret;
            }
            if (block == Core.registry.lightair_block) {
                if (md == BlockLightAir.air_md) {
                    return false;
                }
                return true;
            }
            return false;
        } finally {
            Core.profileEnd();
        }
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return Core.factory_rendertype;
    }
}
