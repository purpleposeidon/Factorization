package factorization.client.render;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ModLoader;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.forge.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import factorization.api.Coord;
import factorization.api.IFactoryType;
import factorization.common.BlockFactorization;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.RenderingCube;
import factorization.common.RenderingCube.Vector;
import factorization.common.Texture;
import factorization.common.TileEntitySolarTurbine;
import factorization.common.TileEntityWire;
import factorization.common.WireConnections;

public class FactorizationRender {
    static Block metal = Block.obsidian;
    static Block glass = Block.glowStone;

    static boolean world_mode;
    static public int x, y, z;

    static void renderInWorld(int wx, int wy, int wz) {
        world_mode = true;
        x = wx;
        y = wy;
        z = wz;
    }

    static void renderInInventory() {
        world_mode = false;
    }

    static void renderLamp(RenderBlocks rb, int handleSide) {
        float s = 1F / 16F;
        float p = 1F / 64F;
        float trim_out = BlockFactorization.lamp_pad;
        float trim_in = trim_out + s * 2;
        float glass_mid = (trim_in + trim_out) / 2;
        float glass_ver = trim_in; //trim_in + 1F / 128F;
        float panel = trim_out + s; //trim_in + s * 0;
        BlockFactorization block = Core.registry.factory_block;
        int metal = Texture.lamp_iron;
        int glass = Texture.lamp_iron + 2;
        //glass
        renderPart(rb, glass, glass_mid, glass_ver, glass_mid, 1 - glass_mid, 1 - glass_ver, 1 - glass_mid);
        //corners
        renderPart(rb, metal, trim_in, trim_in, trim_in, trim_out, 1 - trim_in, trim_out); //lower left
        renderPart(rb, metal, 1 - trim_out, trim_in, 1 - trim_out, 1 - trim_in, 1 - trim_in, 1 - trim_in); //upper right
        renderPart(rb, metal, trim_in, 1 - trim_in, 1 - trim_in, trim_out, trim_in, 1 - trim_out); //upper left
        renderPart(rb, metal, 1 - trim_in, 1 - trim_in, trim_in, 1 - trim_out, trim_in, trim_out); //lower right
        //covers
        renderPart(rb, metal, trim_out, 1 - trim_in, trim_out, 1 - trim_out, 1 - trim_out, 1 - trim_out); //top
        renderPart(rb, metal, 1 - trim_out, trim_out, 1 - trim_out, trim_out, trim_in, trim_out); //bottom
        //knob
        renderPart(rb, metal, panel, 1 - trim_out, panel, 1 - panel, 1 - trim_out + s * 1, 1 - panel);
        renderPart(rb, metal, panel, trim_out - s * 1, panel, 1 - panel, trim_out, 1 - panel);

        //TODO: Handle. From the top, a side, or the ground.
    }

    static void renderFire(RenderBlocks rb) {
        //do nothing?
        rb.renderBlockFire(Block.fire, x, y, z);
    }

    static void renderSentryDemon(RenderBlocks rb) {
        BlockFactorization block = Core.registry.factory_block;
        int cage = block.getBlockTextureFromSideAndMetadata(0, FactoryType.SENTRYDEMON.md);
        float h = 0.99F, l = 0.01F;
        renderPart(rb, cage, h, h, h, l, l, l);
        renderPart(rb, cage, l, l, l, h, h, h);
    }

    static void renderSolarTurbine(RenderBlocks rb, int water_height) {
        int glass = Texture.lamp_iron + 2;
        int water = 7;
        float d = 1F / 16F;
        if (MinecraftForgeClient.getRenderPass() == 1) {
            if (water_height <= 1F / 16F) {
                return;
            }
            //Tessellator.instance.setColorOpaque_F(0.5F, 0.5F, 0.5F);
            //Tessellator.instance.setColorOpaque(255, 0, 255);
            renderPart(rb, 7, d, 0.001F, d, 1 - d, (1 + water_height / (TileEntitySolarTurbine.max_water / 4)) / 16F, 1 - d);
            //			renderPart(rb, glass, 1 - d, 1 - d, 1 - d, d, 0.02F, d);
            return;
        }
        if (true) {
            renderPart(rb, glass, 0, 0, 0, 1, 1, 1);
        }
        if (!world_mode) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-0.5F, 0.1F, -0.5F);
            GL11.glRotatef(90, 1, 0, 0);
            renderItemIn2D(10);
            GL11.glPopMatrix();
        }
        renderMotor(rb);
    }

    static void renderMotor(RenderBlocks rb) {
        int lead = Core.registry.lead_block_item.getIconIndex();
        float d = 4.0F / 16.0F;
        float yd = -d + 0.003F;
        renderPart(rb, lead, d, d + yd, d, 1 - d, 1 - d + yd, 1 - d);
    }

    static void renderMirrorStand(RenderBlocks rb) {
        float height = 6.5F / 16F;
        float radius = 1F / 16F;
        float c = 0.5F;
        renderPart(rb, Texture.silver, c - radius, 0, c - radius, c + radius, height, c + radius);
        float trim = 3F / 16F;
        float trim_height = 2F / 16F;
        renderPart(rb, Texture.silver, trim, 0, trim, 1 - trim, trim_height, 1 - trim);
    }

    static float wireWidth = 4F / 16F;


    final static double uvd = -0.00001;

    static void plane(int icon, int ox, int oy, int oz, int width, int height, int depth) {
        Tessellator tes = Tessellator.instance;
        double u = ((icon & 0xf) << 4) / 256.0;
        double v = (icon & 0xf0) / 256.0;

        double ax = x + ox / 16F, ay = y + oy / 16F, az = z + oz / 16F;
        double bx = ax + width / 16F, by = ay + height / 16F, bz = az + depth / 16F;

        tes.addVertexWithUV(ax, ay, az, u, v);
        double uw = u; // u + uvd;
        double vw = v; //v + uvd;
        if (width == 0) {
            uw += height / 256F;
            vw += depth / 256F;
            tes.addVertexWithUV(ax, by, az, uw, v);
            tes.addVertexWithUV(ax, by, bz, uw, vw);
            tes.addVertexWithUV(ax, ay, bz, u, vw);
        } else if (height == 0) {
            uw += width / 256F;
            vw += depth / 256F;
            tes.addVertexWithUV(ax, ay, bz, u, vw);
            tes.addVertexWithUV(bx, ay, bz, uw, vw);
            tes.addVertexWithUV(bx, ay, az, uw, v);
        } else if (depth == 0) {
            uw += width / 256F;
            vw += height / 256F;
            tes.addVertexWithUV(ax, by, az, u, vw);
            tes.addVertexWithUV(bx, by, az, uw, vw);
            tes.addVertexWithUV(bx, ay, az, uw, v);
        }
    }

    static WireRenderer wireRenderer = new WireRenderer();
    static void renderWireWorld(RenderBlocks rb, Coord me) {
        for (RenderingCube rc : new WireConnections(me.getTE(TileEntityWire.class)).getParts()) {
            renderCube(rc);
        }
    }

    static void renderWireInv(RenderBlocks rb) {
        int lead = Core.registry.resource_block.getBlockTextureFromSideAndMetadata(0, Core.registry.lead_block_item.getItemDamage());
        renderPart(rb, lead, 0, 0, 0, 1, wireWidth, wireWidth);
        renderPart(rb, lead, 0, 0, 0, wireWidth, wireWidth, 1);
    }

    public static void renderNormalBlock(RenderBlocks rb, int x, int y, int z, int md) {
        if (world_mode) {
            Block b = Core.registry.factory_block;
            rb.renderStandardBlock(b, x, y, z);
        }
        else {
            Core.registry.factory_block.fake_normal_render = true;
            rb.renderBlockAsItem(Core.registry.factory_block, md, 1.0F);
            Core.registry.factory_block.fake_normal_render = false;
        }
    }

    private static void renderPart(RenderBlocks rb, int texture, float b1, float b2, float b3,
            float b4, float b5, float b6) {
        BlockFactorization block = Core.registry.factory_block;
        block.setBlockBounds(b1, b2, b3, b4, b5, b6);
        if (world_mode) {
            Texture.force_texture = texture;
            rb.renderStandardBlock(block, x, y, z);
            Texture.force_texture = -1;
        }
        else {
            renderPartInvTexture(rb, block, texture);
        }
        block.setBlockBounds(0, 0, 0, 1, 1, 1);
    }

    private static void renderPartInvTexture(RenderBlocks renderblocks,
            Block block, int texture) {
        // This originally copied from RenderBlocks.renderBlockAsItem
        Tessellator tessellator = Tessellator.instance;

        block.setBlockBoundsForItemRender();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, -1F, 0.0F);
        renderblocks.renderBottomFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 1.0F, 0.0F);
        renderblocks.renderTopFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, -1F);
        renderblocks.renderEastFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(0.0F, 0.0F, 1.0F);
        renderblocks.renderWestFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(-1F, 0.0F, 0.0F);
        renderblocks.renderNorthFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        tessellator.startDrawingQuads();
        tessellator.setNormal(1.0F, 0.0F, 0.0F);
        renderblocks.renderSouthFace(block, 0.0D, 0.0D, 0.0D, texture);
        tessellator.draw();
        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
    }

    static public boolean renderWorldBlock(RenderBlocks renderBlocks, IBlockAccess world,
            int x, int y, int z, Block block, int render_type) {
        boolean first_pass = MinecraftForgeClient.getRenderPass() == 0;
        FactorizationRender.renderInWorld(x, y, z);
        int md = world.getBlockMetadata(x, y, z);
        if (block == Core.registry.factory_block) {
            TileEntity te = world.getBlockTileEntity(x, y, z);
            if (te instanceof IFactoryType) {
                md = ((IFactoryType) te).getFactoryType().md;
            } else {
                md = -1;
            }
            if (FactoryType.SOLARTURBINE.is(md)) {
                renderSolarTurbine(renderBlocks, ((TileEntitySolarTurbine) te).water_level);
                return true;
            }
            if (!first_pass) {
                return false;
            }
            if (FactoryType.LAMP.is(md)) {
                //TODO: Pick a side for the handle to go on
                renderLamp(renderBlocks, 0);
            } else if (FactoryType.SENTRYDEMON.is(md)) {
                renderSentryDemon(renderBlocks);
            } else if (FactoryType.MIRROR.is(md)) {
                renderMirrorStand(renderBlocks);
            } else if (FactoryType.LEADWIRE.is(md)) {
                renderWireWorld(renderBlocks, new Coord(ModLoader.getMinecraftInstance().theWorld, x, y, z));
            } else {
                renderNormalBlock(renderBlocks, x, y, z, md);
            }
            return true;
        }
        if (block == Core.registry.lightair_block) {
            if (md == Core.registry.lightair_block.air_md) {
                return false;
            }
            if (md == Core.registry.lightair_block.fire_md) {
                renderFire(renderBlocks);
            }
            return true;
        }
        return false;
    }

    public static void renderInvBlock(RenderBlocks renderBlocks, Block block, int damage,
            int render_type) {
        if (block == Core.registry.factory_block) {
            FactorizationRender.renderInInventory();
            if (FactoryType.LAMP.is(damage)) {
                FactorizationRender.renderLamp(renderBlocks, 0);
            } else if (FactoryType.SOLARTURBINE.is(damage)) {
                renderSolarTurbine(renderBlocks, 0);
            } else if (FactoryType.LEADWIRE.is(damage)) {
                renderWireInv(renderBlocks);
            } else {
                renderNormalBlock(renderBlocks, 0, 0, 0, damage);
            }
            if (FactoryType.HEATER.is(damage)) {
                float d = 1F / 32F;
                GL11.glColor3f(0.1F, 0.1F, 0.1F);
                renderPart(renderBlocks, Texture.heater_element, d, d, d, 1 - d, 1 - d, 1 - d);
            }
        }
    }

    static void renderItemIn2D(int icon_index) {
        float var6 = ((float) (icon_index % 16 * 16) + 0.0F) / 256.0F;
        float var7 = ((float) (icon_index % 16 * 16) + 15.9999F) / 256.0F;
        float var8 = ((float) (icon_index / 16 * 16) + 0.0F) / 256.0F;
        float var9 = ((float) (icon_index / 16 * 16) + 15.9999F) / 256.0F;

        renderItemIn2D_DO(Tessellator.instance, var7, var8, var6, var9);
    }

    //copied from ItemRenderer.renderItemIn2D()
    static void renderItemIn2D_DO(Tessellator par1Tessellator, float par2, float par3, float par4,
            float par5) {
        float var6 = 1.0F;
        float var7 = 0.0625F;
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 0.0F, 1.0F);
        par1Tessellator.addVertexWithUV(0.0D, 0.0D, 0.0D, (double) par2, (double) par5);
        par1Tessellator.addVertexWithUV((double) var6, 0.0D, 0.0D, (double) par4, (double) par5);
        par1Tessellator.addVertexWithUV((double) var6, 1.0D, 0.0D, (double) par4, (double) par3);
        par1Tessellator.addVertexWithUV(0.0D, 1.0D, 0.0D, (double) par2, (double) par3);
        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 0.0F, -1.0F);
        par1Tessellator.addVertexWithUV(0.0D, 1.0D, (double) (0.0F - var7), (double) par2, (double) par3);
        par1Tessellator.addVertexWithUV((double) var6, 1.0D, (double) (0.0F - var7), (double) par4, (double) par3);
        par1Tessellator.addVertexWithUV((double) var6, 0.0D, (double) (0.0F - var7), (double) par4, (double) par5);
        par1Tessellator.addVertexWithUV(0.0D, 0.0D, (double) (0.0F - var7), (double) par2, (double) par5);
        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(-1.0F, 0.0F, 0.0F);
        int var8;
        float var9;
        float var10;
        float var11;

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par2 + (par4 - par2) * var9 - 0.001953125F;
            var11 = var6 * var9;
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, (double) (0.0F - var7), (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, 0.0D, (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, 0.0D, (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, (double) (0.0F - var7), (double) var10, (double) par3);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(1.0F, 0.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par2 + (par4 - par2) * var9 - 0.001953125F;
            var11 = var6 * var9 + 0.0625F;
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, (double) (0.0F - var7), (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 1.0D, 0.0D, (double) var10, (double) par3);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, 0.0D, (double) var10, (double) par5);
            par1Tessellator.addVertexWithUV((double) var11, 0.0D, (double) (0.0F - var7), (double) var10, (double) par5);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, 1.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par5 + (par3 - par5) * var9 - 0.001953125F;
            var11 = var6 * var9 + 0.0625F;
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, 0.0D, (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, 0.0D, (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, (double) (0.0F - var7), (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, (double) (0.0F - var7), (double) par2, (double) var10);
        }

        par1Tessellator.draw();
        par1Tessellator.startDrawingQuads();
        par1Tessellator.setNormal(0.0F, -1.0F, 0.0F);

        for (var8 = 0; var8 < 16; ++var8)
        {
            var9 = (float) var8 / 16.0F;
            var10 = par5 + (par3 - par5) * var9 - 0.001953125F;
            var11 = var6 * var9;
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, 0.0D, (double) par4, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, 0.0D, (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV(0.0D, (double) var11, (double) (0.0F - var7), (double) par2, (double) var10);
            par1Tessellator.addVertexWithUV((double) var6, (double) var11, (double) (0.0F - var7), (double) par4, (double) var10);
        }

        par1Tessellator.draw();
    }

    static public void renderCube(RenderingCube rc) {
        for (int face = 0; face < 6; face++) {
            Vector[] vecs = rc.faceVerts(face);
            for (int i = 0; i < vecs.length; i++) {
                Vector vec = vecs[i];
                vertex(rc, vec.x, vec.y, vec.z, vec.u, vec.v);
            }
        }
    }

    public static void initRenderCube() {
        //		Tessellator.instance.setBrightness(0xf000f);
    }

    static private void vertex(RenderingCube rc, float x, float y, float z, float u, float v) {
        //all units are in texels; center of the cube is the origin. Or, like... not the center but the texel that's (8,8,8) away from the corner is.
        //u & v are in texels
        Tessellator.instance.setColorOpaque_F(1, 1, 1);
        Tessellator.instance.addVertexWithUV(
                FactorizationRender.x + 0.5 + x / 16F,
                FactorizationRender.y + 0.5 + y / 16F,
                FactorizationRender.z + 0.5 + z / 16F,
                rc.ul + u / 256F, rc.vl + v / 256F);
    }
}
