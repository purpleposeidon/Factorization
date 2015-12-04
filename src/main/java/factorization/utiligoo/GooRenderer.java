package factorization.utiligoo;

import factorization.common.BlockIcons;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.util.RenderUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

public enum GooRenderer {
    INSTANCE;
    Minecraft mc = Minecraft.getMinecraft();
    ShaderManager sobel = null;
    boolean loaded = false;
    
    javax.vecmath.Matrix4f projectionMatrix;
    
    void resetProjectionMatrix() {
        projectionMatrix = new javax.vecmath.Matrix4f();
        projectionMatrix.setIdentity();
        projectionMatrix.m00 = 2.0F / (float)mc.displayWidth;
        projectionMatrix.m11 = 2.0F / (float)(-mc.displayHeight);
        projectionMatrix.m22 = -0.0020001999F;
        projectionMatrix.m33 = 1.0F;
        projectionMatrix.m03 = -1.0F;
        projectionMatrix.m13 = 1.0F;
        projectionMatrix.m23 = -1.0001999F;
    }

    
    private boolean useShaders() {
        return false;
/*        if (loaded) return sobel != null;
        loaded = true;
        try {
            sobel = new ShaderManager(mc.getResourceManager(), "invert");
            sobel.func_147992_a("DiffuseSampler", mc.getFramebuffer());
        } catch (IOException e) {
            e.printStackTrace();
            sobel = null;
            return false;
        }
        return true;*/
    }
    
    
    private void beginGlWithShaders() {
        resetProjectionMatrix();
        int width = mc.getFramebuffer().framebufferWidth;
        int height = mc.getFramebuffer().framebufferHeight;
        //sobel.func_147984_b("ProjMat").setProjectionMatrix(projectionMatrix);
        //sobel.func_147984_b("InSize").func_148087_a((float)width, (float)height);
        //sobel.func_147984_b("OutSize").func_148087_a(width, height);
        //sobel.func_147984_b("Time").func_148090_a(0);
        sobel.func_147995_c();
    }
    
    private void endGlWithShaders() {
        sobel.func_147993_b();
    }
    
    private void beginGlNoShaders() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4d(1, 1, 1, 0.9);
        OpenGlHelper.glBlendFunc(774, 768, 1, 0);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(0F, -100F);
    }
    
    private void endGlNoShaders() {
        GL11.glPolygonOffset(0.0F, 0.0F);
        GL11.glPopAttrib();
    }
    
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void renderGoo(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null) return;
        boolean rendered_something = false;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack is = player.inventory.getStackInSlot(slot);
            if (is == null || is.getItem() != Core.registry.utiligoo) continue;
            GooData data = GooData.getNullGooData(is, mc.theWorld);
            if (data == null) continue; 
            if (data.dimensionId != mc.theWorld.provider.dimensionId) continue;
            if (data.coords.length == 0) continue;
            if (!rendered_something) {
                rendered_something = true;
                EntityLivingBase camera = Minecraft.getMinecraft().renderViewEntity;
                double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks;
                double cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks;
                double cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks;
                mc.renderEngine.bindTexture(Core.blockAtlas);
                GL11.glPushMatrix();
                GL11.glTranslated(-cx, -cy, -cz);
                if (useShaders()) {
                    beginGlWithShaders();
                } else {
                    beginGlNoShaders();
                }
            }
            renderGooFor(event, data, player);
        }
        if (rendered_something) {
            GL11.glPopMatrix();
            if (useShaders()) {
                endGlWithShaders();
            } else {
                endGlNoShaders();
            }
        }
    }
    
    @SideOnly(Side.CLIENT)
    void renderGooFor(RenderWorldLastEvent event, GooData data, EntityPlayer player) {
        boolean rendered_something = false;
        double render_dist_sq = 32*32;
        Tessellator tess = Tessellator.instance;
        BlockRenderHelper block = BlockRenderHelper.instance;
        RenderBlocks rb = RenderUtil.getRB();
        for (int i = 0; i < data.coords.length; i += 3) {
            int x = data.coords[i + 0];
            int y = data.coords[i + 1];
            int z = data.coords[i + 2];
            if (player.getDistanceSq(x, y, z) > render_dist_sq) continue;
            if (!rendered_something) {
                tess.startDrawingQuads();
                tess.disableColor();
                rendered_something = true;
            }
            Block b = player.worldObj.getBlock(x, y, z);
            Material mat = b.getMaterial();
            int md = player.worldObj.getBlockMetadata(x, y, z);
            if (useShaders()) {
                if (mat.blocksMovement() && !b.hasTileEntity(md)) {
                    rb.renderBlockByRenderType(b, x, y, z);
                } else {
                    b.setBlockBoundsBasedOnState(player.world, pos);
                    block.setBlockBounds((float)b.getBlockBoundsMinX(), (float)b.getBlockBoundsMinY(), (float)b.getBlockBoundsMinZ(), (float)b.getBlockBoundsMaxX(), (float)b.getBlockBoundsMaxY(), (float)b.getBlockBoundsMaxZ()); // Hello, Notch! 
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                }
            } else {
                if (mat.blocksMovement() && !b.hasTileEntity(md)) {
                    rb.renderBlockUsingTexture(b, x, y, z, BlockIcons.utiligoo$invasion);
                } else if (b.getRenderType() == 2 /* torches */) {
                    // Torches stupidly don't support setBlockBoundsBasedOnState. #blamenotch
                    float d = 0.25F;
                    block.setBlockBoundsOffset(d, d, d);
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                } else {
                    b.setBlockBoundsBasedOnState(player.world, pos);
                    block.setBlockBounds((float)b.getBlockBoundsMinX(), (float)b.getBlockBoundsMinY(), (float)b.getBlockBoundsMinZ(), (float)b.getBlockBoundsMaxX(), (float)b.getBlockBoundsMaxY(), (float)b.getBlockBoundsMaxZ()); // Hello, Notch! 
                    block.useTexture(BlockIcons.utiligoo$invasion);
                    block.render(rb, x, y, z);
                }
            }
        }
        if (rendered_something) {
            tess.draw();
        }
    }
}
