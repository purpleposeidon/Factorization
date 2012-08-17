package factorization.client.render;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Block;
import net.minecraft.src.EntityItem;
import net.minecraft.src.FontRenderer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemBlock;
import net.minecraft.src.ItemRedstone;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.OpenGlHelper;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.RenderEngine;
import net.minecraft.src.RenderItem;
import net.minecraft.src.RenderManager;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import factorization.common.TileEntityBarrel;

public class TileEntityBarrelRenderer extends TileEntitySpecialRenderer {

    EntityItem entity = new EntityItem(null);
    RenderItem itemRender; // = new RenderItem();
    Random random = new Random();
    private RenderBlocks renderBlocks = new RenderBlocks();
    static boolean field_27004_a = true;
    float item_rotation = 0;
    boolean render_item, render_text;

    public TileEntityBarrelRenderer(boolean render_item, boolean render_text) {
        this.render_item = render_item;
        this.render_text = render_text;
    }

    @Override
    public void renderTileEntityAt(TileEntity ent, double x, double y, double z,
            float partialTickTime) {
        if (render_item == false && render_text == false) {
            return;
        }
        if (!(ent instanceof TileEntityBarrel)) {
            return;
        }
        TileEntityBarrel barrel = (TileEntityBarrel) ent;
        if (barrel.item == null || barrel.getItemCount() == 0) {
            return;
        }

        entity.item = barrel.item;
        double idy = 0.40;
        itemRender = (RenderItem) RenderManager.instance.getEntityClassRenderObject(EntityItem.class);
        final double d = 0.01;

        //GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        switch (barrel.facing_direction) {
        case 3:
            item_rotation = 0;
            setupLight(barrel, 0, 1);
            if (render_item)
                handleRenderItem(entity, x + 0.5, y + idy, z + 1 + d, 0, 0, 2);
            if (render_text)
                renderItemCount(barrel, 2, x, y, z);
            break;

        case 2:
            item_rotation = -180;
            setupLight(barrel, 0, -1);
            if (render_item)
                handleRenderItem(entity, x + 0.5, y + idy, z - d, 0, 0, 0);
            if (render_text)
                renderItemCount(barrel, 0, x, y, z);
            break;
        case 4:
            item_rotation = -90;
            setupLight(barrel, -1, 0);
            if (render_item)
                handleRenderItem(entity, x - d, y + idy, z + 0.5, 0, 0, 1);
            if (render_text)
                renderItemCount(barrel, 1, x, y, z);
            break;
        case 5:
        default: //Need to keep a default in case something is messed up
            item_rotation = 90;
            setupLight(barrel, 1, 0);
            if (render_item)
                handleRenderItem(entity, x + 1 + d, y + idy, z + 0.5, 0, 0, 3);
            if (render_text)
                renderItemCount(barrel, 3, x, y, z);
            break;
        }
    }

    void setupLight(TileEntityBarrel barrel, int dx, int dz) {
        int br = barrel.getCoord().w.getLightBrightnessForSkyBlocks(barrel.xCoord + dx, barrel.yCoord,
                barrel.zCoord + dz, 0 /* minimum */);
        br = (int)(br * 0.55);
        int var11 = br % 65536;
        int var12 = br / 65536;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) var11 / 1.0F,
                (float) var12 / 1.0F);
    }

    void renderItemCount(TileEntityBarrel barrel, int side, double x, double y, double z) {
        FontRenderer fontRender = getFontRenderer();
        GL11.glPushMatrix();
        GL11.glTranslated(x + 0.5, y + .85, z + 0.5);
        GL11.glRotatef(90 * side, 0, 1, 0);
        GL11.glTranslated(0, 0, -0.51);
        GL11.glDepthMask(false);

        float scale = 0.01F;
        GL11.glScalef(scale, scale, scale);
        GL11.glRotatef(180, 0, 0, 1);

        String t = "";
        int ms = barrel.item.getMaxStackSize();
        if (ms == 1 || barrel.getItemCount() == ms) {
            t += barrel.getItemCount();
        } else {
            int q = barrel.getItemCount() / ms;
            if (q > 0) {
                t += (barrel.getItemCount() / ms) + "*" + ms;
            }
            int r = (barrel.getItemCount() % ms);
            if (r != 0) {
                if (q > 0) {
                    t += " + ";
                }
                t += r;
            }
        }
        int color = 0xbbbbbb;
        if (barrel.canExplode()) {
            t = "!! " + t + " !!";
            //			if (barrel.worldObj.getWorldTime() / 40 % 2 == 0 && barrel.flamingExplosion()) {
            //				color = 0xccaaaa;
            //			}
        }
        // XXX TODO: This sometimes renders too bright. Probably some OpenGL
        // state that some other part of the render is changing. Any ideas?
        // some possibilities: +TEXTURE_2D +BLEND ±LIGHTING
        fontRender.drawString(t, -fontRender.getStringWidth(t) / 2, 0, color);
        // Seems like using anything other than 0 for alpha fucks shit up?
        // That function is ass. I hate it.

        GL11.glDepthMask(true);
        GL11.glPopMatrix();
    }

    Random rand = new Random();

    public void handleRenderItem(EntityItem entity, double x, double y, double z, float par8,
            float par9, int side) {
        ItemStack var10 = entity.item;
        doRenderItem(entity, x, y, z, par8, par9);
        //doRenderItem(entity, x, y, z, par8, par9);
    }

    
    
    //stole from RenderItem.doRenderItem (!!! *not* ItemRender !!!)
    public void doRenderItem(EntityItem par1EntityItem, double par2, double par4, double par6, float par8, float par9)
    {
        this.random.setSeed(187L);
        ItemStack var10 = par1EntityItem.item;
        GL11.glPushMatrix();
        float var11 = MathHelper.sin(((float)par1EntityItem.age + par9) / 10.0F + par1EntityItem.hoverStart) * 0.1F + 0.1F;
        float var12 = (((float)par1EntityItem.age + par9) / 20.0F + par1EntityItem.hoverStart) * (180F / (float)Math.PI);

        GL11.glTranslatef((float)par2, (float)par4 + var11, (float)par6);
        GL11.glPushMatrix(); //these 2 transforms aren't needed for normal items
        GL11.glRotatef(item_rotation - 90, 0.0F, 1.0F, 0.0F);
        GL11.glScalef(0.05F, 1.5F, 1.5F);
        GL11.glRotatef(90/4, 0, 0, -1);
        GL11.glRotatef(45, 0, 1, 0);
        GL11.glTranslatef(0, -0.05F, 0);
//        GL11.glTranslatef(1, 0, 0);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        int var16;
        float var19;
        float var20;
        float var24;

        if (ForgeHooksClient.renderEntityItem(par1EntityItem, var10, var11, var12, random, Minecraft.getMinecraft().renderEngine, renderBlocks))
        {
           GL11.glPopMatrix();
        }
        else if (var10.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.blocksList[var10.itemID].getRenderType()))
        {
            this.loadTexture(Block.blocksList[var10.itemID].getTextureFile());
            float var22 = 0.25F;
            var16 = Block.blocksList[var10.itemID].getRenderType();

            if (var16 == 1 || var16 == 19 || var16 == 12 || var16 == 2)
            {
                var22 = 0.5F;
            }

            GL11.glScalef(var22, var22, var22);

            var24 = 1.0F;
            this.renderBlocks.renderBlockAsItem(Block.blocksList[var10.itemID], var10.getItemDamage(), var24);
            GL11.glPopMatrix();
        }
        else
        {
            GL11.glPopMatrix();
            GL11.glRotatef(item_rotation, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0, -0.1F, 0);
            int var15;
            float var17;
            if (var10.getItem().requiresMultipleRenderPasses())
            {
                GL11.glScalef(0.5F, 0.5F, 0.5F);
                this.loadTexture(Item.itemsList[var10.itemID].getTextureFile());

                for (var15 = 0; var15 <= var10.getItem().getRenderPasses(var10.getItemDamage()); ++var15)
                {
                    this.random.setSeed(187L); //Fixes Vanilla bug where layers would not render aligns properly.
                    var16 = var10.getItem().getIconFromDamageForRenderPass(var10.getItemDamage(), var15);
                    var17 = 1.0F;
                    this.func_77020_a(var16, 1);
                }
            }
            else
            {
                GL11.glScalef(0.5F, 0.5F, 0.5F);
                var15 = var10.getIconIndex();

                this.loadTexture(var10.getItem().getTextureFile());

                this.func_77020_a(var15, 1);
            }
        }

        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        GL11.glPopMatrix();
    }

    private void func_77020_a(int par1, int par2)
    {
        Tessellator var3 = Tessellator.instance;
        float var4 = (float)(par1 % 16 * 16 + 0) / 256.0F;
        float var5 = (float)(par1 % 16 * 16 + 16) / 256.0F;
        float var6 = (float)(par1 / 16 * 16 + 0) / 256.0F;
        float var7 = (float)(par1 / 16 * 16 + 16) / 256.0F;
        float var8 = 1.0F;
        float var9 = 0.5F;
        float var10 = 0.25F;

        GL11.glPushMatrix();
        GL11.glTranslatef(0, -0.2F, 0);

        //GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        var3.startDrawingQuads();
        var3.setNormal(0.0F, 1.0F, 0.0F);
        var3.addVertexWithUV((double)(0.0F - var9), (double)(0.0F - var10), 0.0D, (double)var4, (double)var7);
        var3.addVertexWithUV((double)(var8 - var9), (double)(0.0F - var10), 0.0D, (double)var5, (double)var7);
        var3.addVertexWithUV((double)(var8 - var9), (double)(1.0F - var10), 0.0D, (double)var5, (double)var6);
        var3.addVertexWithUV((double)(0.0F - var9), (double)(1.0F - var10), 0.0D, (double)var4, (double)var6);
        var3.draw();
        GL11.glPopMatrix();
    }
    
    protected void loadTexture(String par1Str)
    {
        RenderEngine var2 = Minecraft.getMinecraft().renderEngine;
        var2.bindTexture(var2.getTexture(par1Str));
    }

    
//	public void doRenderItem(EntityItem par1EntityItem, double par2, double par4, double par6,
//			float par8, float par9) {
//		ItemStack var10 = par1EntityItem.item;
//		GL11.glPushMatrix();
//		float var12 = 0, var11 = 0;
//		byte var13 = 1;
//
//		GL11.glTranslatef((float) par2, (float) par4 /* + var11 */, (float) par6);
//		GL11.glEnable(GL12.GL_RESCALE_NORMAL);
//		int var15;
//		float var19;
//		float var18;
//		float var23;
//
//		IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(var10, ENTITY);
//
//		if (customRenderer != null) {
//			//			if (customRenderer.shouldUseRenderHelper(ENTITY, var10, ENTITY_ROTATION)) {
//			//				GL11.glRotatef(var12, 0.0F, 1.0F, 0.0F);
//			//			}
//			//			if (!customRenderer.shouldUseRenderHelper(ENTITY, var10, ENTITY_BOBBING)) {
//			//				GL11.glTranslatef(0.0F, -var11, 0.0F);
//			//			}
//			boolean is3D = customRenderer.shouldUseRenderHelper(ENTITY, var10, BLOCK_3D);
//
//			if (var10.itemID < 256
//					&& (is3D || RenderBlocks.renderItemIn3d(Block.blocksList[var10.itemID]
//							.getRenderType()))) {
//				mod_Factorization.loadTexture(itemRender, ForgeHooksClient.getTexture("/terrain.png", var10.getItem()));
//				float var21 = 0.25F;
//				var15 = Block.blocksList[var10.itemID].getRenderType();
//
//				if (var15 == 1 || var15 == 19 || var15 == 12 || var15 == 2) {
//					var21 = 0.5F;
//				}
//
//				GL11.glScalef(var21, var21, var21);
//
//				for (int j = 0; j < var13; j++) {
//					GL11.glPushMatrix();
//					if (j > 0) {
//						GL11.glTranslatef(((random.nextFloat() * 2.0F - 1.0F) * 0.2F) / 0.5F,
//								((random.nextFloat() * 2.0F - 1.0F) * 0.2F) / 0.5F,
//								((random.nextFloat() * 2.0F - 1.0F) * 0.2F) / 0.5F);
//					}
//					customRenderer.renderItem(ENTITY, var10, renderBlocks, par1EntityItem);
//					GL11.glPopMatrix();
//				}
//			} else {
//				mod_Factorization.loadTexture(itemRender,
//						ForgeHooksClient.getTexture(var10.itemID < 256 ? "/terrain.png" : "/gui/items.png", var10.getItem()));
//				GL11.glScalef(0.5F, 0.5F, 0.5F);
//				customRenderer.renderItem(ENTITY, var10, renderBlocks, par1EntityItem);
//			}
//
//		} else if (var10.itemID < 256
//				&& RenderBlocks.renderItemIn3d(Block.blocksList[var10.itemID].getRenderType())) {
//			GL11.glRotatef(var12, 0.0F, 1.0F, 0.0F);
//			mod_Factorization.loadTexture(itemRender,
//					ForgeHooksClient.getTexture("/terrain.png", Block.blocksList[var10.itemID]));
//			float var21 = 0.25F;
//			var15 = Block.blocksList[var10.itemID].getRenderType();
//
//			if (var15 == 1 || var15 == 19 || var15 == 12 || var15 == 2) {
//				var21 = 0.5F;
//			}
//
//			GL11.glScalef(var21, var21, var21);
//
//			for (int var22 = 0; var22 < var13; ++var22) {
//				GL11.glPushMatrix();
//
//				if (var22 > 0) {
//					var23 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var21;
//					var18 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var21;
//					var19 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var21;
//					GL11.glTranslatef(var23, var18, var19);
//				}
//
//				var23 = 1.0F;
//				this.renderBlocks.renderBlockAsItem(Block.blocksList[var10.itemID],
//						var10.getItemDamage(), var23);
//				GL11.glPopMatrix();
//			}
//		} else {
//			int var14;
//			float var16;
//
//			//if (var10.getItem().func_46058_c()) {
//			if (var10.getItem().requiresMultipleRenderPasses()) {
//				GL11.glScalef(0.5F, 0.5F, 0.5F);
//				mod_Factorization.loadTexture(itemRender,
//						ForgeHooksClient.getTexture("/gui/items.png", Item.itemsList[var10.itemID]));
//
//				for (var14 = 0; var14 < var10.getItem().getRenderPasses(var10.getItemDamage()); ++var14) {
//					this.random.setSeed(187L); // Fixes Vanilla bug where layers would not render aligns properly.
//					//func_46057_a
//					//getIconFromDamageForRenderPass
//					var15 = var10.getItem().func_46057_a(var10.getItemDamage(), var14);
//					var16 = 1.0F;
//
//					if (this.field_27004_a) {
//						int var17 = Item.itemsList[var10.itemID].getColorFromDamage(
//								var10.getItemDamage(), var14);
//						var18 = (float) (var17 >> 16 & 255) / 255.0F;
//						var19 = (float) (var17 >> 8 & 255) / 255.0F;
//						float var20 = (float) (var17 & 255) / 255.0F;
//						GL11.glColor4f(var18 * var16, var19 * var16, var20 * var16, 1.0F);
//					}
//
//					this.func_40267_a(var15, var13);
//				}
//			} else {
//				GL11.glScalef(0.5F, 0.5F, 0.5F);
//				var14 = var10.getIconIndex();
//
//				if (var10.itemID < 256) {
//					mod_Factorization.loadTexture(itemRender,
//							ForgeHooksClient.getTexture("/terrain.png", Block.blocksList[var10.itemID]));
//				} else {
//					mod_Factorization.loadTexture(itemRender,
//							ForgeHooksClient.getTexture("/gui/items.png", Item.itemsList[var10.itemID]));
//				}
//
//				if (this.field_27004_a) {
//					var15 = Item.itemsList[var10.itemID].getColorFromDamage(var10.getItemDamage(),
//							0);
//					var16 = (float) (var15 >> 16 & 255) / 255.0F;
//					var23 = (float) (var15 >> 8 & 255) / 255.0F;
//					var18 = (float) (var15 & 255) / 255.0F;
//					var19 = 1.0F;
//					GL11.glColor4f(var16 * var19, var23 * var19, var18 * var19, 1.0F);
//				}
//
//				this.func_40267_a(var14, var13);
//			}
//		}
//
//		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
//		GL11.glPopMatrix();
//	}
//
//	private void func_40267_a(int par1, int par2) {
//		Tessellator var3 = Tessellator.instance;
//		float var4 = (float) (par1 % 16 * 16 + 0) / 256.0F;
//		float var5 = (float) (par1 % 16 * 16 + 16) / 256.0F;
//		float var6 = (float) (par1 / 16 * 16 + 0) / 256.0F;
//		float var7 = (float) (par1 / 16 * 16 + 16) / 256.0F;
//		float var8 = 1.0F;
//		float var9 = 0.5F;
//		float var10 = 0.25F;
//
//		for (int var11 = 0; var11 < par2; ++var11) {
//			GL11.glPushMatrix();
//
//			if (var11 > 0) {
//				float var12 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//				float var13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//				float var14 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//				GL11.glTranslatef(var12, var13, var14);
//			}
//
//			GL11.glRotatef(item_rotation, 0.0F, 1.0F, 0.0F);
//			var3.startDrawingQuads();
//			var3.setNormal(0.0F, 1.0F, 0.0F);
//			var3.addVertexWithUV((double) (0.0F - var9), (double) (0.0F - var10), 0.0D,
//					(double) var4, (double) var7);
//			var3.addVertexWithUV((double) (var8 - var9), (double) (0.0F - var10), 0.0D,
//					(double) var5, (double) var7);
//			var3.addVertexWithUV((double) (var8 - var9), (double) (1.0F - var10), 0.0D,
//					(double) var5, (double) var6);
//			var3.addVertexWithUV((double) (0.0F - var9), (double) (1.0F - var10), 0.0D,
//					(double) var4, (double) var6);
//			var3.draw();
//			GL11.glPopMatrix();
//		}
//	}
//	
//	
    
    
//    public void doRenderItem(EntityItem par1EntityItem, double par2, double par4, double par6, float par8, float par9)
//    {
//        this.random.setSeed(187L);
//        ItemStack var10 = par1EntityItem.item;
//        GL11.glPushMatrix();
//        float var11 = MathHelper.sin(((float)par1EntityItem.age + par9) / 10.0F + par1EntityItem.hoverStart) * 0.1F + 0.1F;
//        float var12 = (((float)par1EntityItem.age + par9) / 20.0F + par1EntityItem.hoverStart) * (180F / (float)Math.PI);
//        byte var13 = 1;
//
//        if (par1EntityItem.item.stackSize > 1)
//        {
//            var13 = 2;
//        }
//
//        if (par1EntityItem.item.stackSize > 5)
//        {
//            var13 = 3;
//        }
//
//        if (par1EntityItem.item.stackSize > 20)
//        {
//            var13 = 4;
//        }
//
//        GL11.glTranslatef((float)par2, (float)par4 + var11, (float)par6);
//        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
//        int var16;
//        float var19;
//        float var20;
//        float var24;
//
//        if (ForgeHooksClient.renderEntityItem(par1EntityItem, var10, var11, var12, random, renderManager.renderEngine, renderBlocks))
//        {
//           ;
//        }
//        else if (var10.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.blocksList[var10.itemID].getRenderType()))
//        {
//            GL11.glRotatef(var12, 0.0F, 1.0F, 0.0F);
//            this.loadTexture(Block.blocksList[var10.itemID].getTextureFile());
//            float var22 = 0.25F;
//            var16 = Block.blocksList[var10.itemID].getRenderType();
//
//            if (var16 == 1 || var16 == 19 || var16 == 12 || var16 == 2)
//            {
//                var22 = 0.5F;
//            }
//
//            GL11.glScalef(var22, var22, var22);
//
//            for (int var23 = 0; var23 < var13; ++var23)
//            {
//                GL11.glPushMatrix();
//
//                if (var23 > 0)
//                {
//                    var24 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var22;
//                    var19 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var22;
//                    var20 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F / var22;
//                    GL11.glTranslatef(var24, var19, var20);
//                }
//
//                var24 = 1.0F;
//                this.renderBlocks.renderBlockAsItem(Block.blocksList[var10.itemID], var10.getItemDamage(), var24);
//                GL11.glPopMatrix();
//            }
//        }
//        else
//        {
//            int var15;
//            float var17;
//
//            if (var10.getItem().requiresMultipleRenderPasses())
//            {
//                GL11.glScalef(0.5F, 0.5F, 0.5F);
//                this.loadTexture(Item.itemsList[var10.itemID].getTextureFile());
//
//                for (var15 = 0; var15 <= var10.getItem().getRenderPasses(var10.getItemDamage()); ++var15)
//                {
//                    this.random.setSeed(187L); //Fixes Vanilla bug where layers would not render aligns properly.
//                    var16 = var10.getItem().getIconFromDamageForRenderPass(var10.getItemDamage(), var15);
//                    var17 = 1.0F;
//
//                    if (this.field_77024_a)
//                    {
//                        int var18 = Item.itemsList[var10.itemID].getColorFromDamage(var10.getItemDamage(), var15);
//                        var19 = (float)(var18 >> 16 & 255) / 255.0F;
//                        var20 = (float)(var18 >> 8 & 255) / 255.0F;
//                        float var21 = (float)(var18 & 255) / 255.0F;
//                        GL11.glColor4f(var19 * var17, var20 * var17, var21 * var17, 1.0F);
//                    }
//
//                    this.func_77020_a(var16, var13);
//                }
//            }
//            else
//            {
//                GL11.glScalef(0.5F, 0.5F, 0.5F);
//                var15 = var10.getIconIndex();
//
//                this.loadTexture(var10.getItem().getTextureFile());
//
//                if (this.field_77024_a)
//                {
//                    var16 = Item.itemsList[var10.itemID].getColorFromDamage(var10.getItemDamage(), 0);
//                    var17 = (float)(var16 >> 16 & 255) / 255.0F;
//                    var24 = (float)(var16 >> 8 & 255) / 255.0F;
//                    var19 = (float)(var16 & 255) / 255.0F;
//                    var20 = 1.0F;
//                    GL11.glColor4f(var17 * var20, var24 * var20, var19 * var20, 1.0F);
//                }
//
//                this.func_77020_a(var15, var13);
//            }
//        }
//
//        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
//        GL11.glPopMatrix();
//    }
//
//    private void func_77020_a(int par1, int par2)
//    {
//        Tessellator var3 = Tessellator.instance;
//        float var4 = (float)(par1 % 16 * 16 + 0) / 256.0F;
//        float var5 = (float)(par1 % 16 * 16 + 16) / 256.0F;
//        float var6 = (float)(par1 / 16 * 16 + 0) / 256.0F;
//        float var7 = (float)(par1 / 16 * 16 + 16) / 256.0F;
//        float var8 = 1.0F;
//        float var9 = 0.5F;
//        float var10 = 0.25F;
//
//        for (int var11 = 0; var11 < par2; ++var11)
//        {
//            GL11.glPushMatrix();
//
//            if (var11 > 0)
//            {
//                float var12 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//                float var13 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//                float var14 = (this.random.nextFloat() * 2.0F - 1.0F) * 0.3F;
//                GL11.glTranslatef(var12, var13, var14);
//            }
//
//            GL11.glRotatef(180.0F - this.renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
//            var3.startDrawingQuads();
//            var3.setNormal(0.0F, 1.0F, 0.0F);
//            var3.addVertexWithUV((double)(0.0F - var9), (double)(0.0F - var10), 0.0D, (double)var4, (double)var7);
//            var3.addVertexWithUV((double)(var8 - var9), (double)(0.0F - var10), 0.0D, (double)var5, (double)var7);
//            var3.addVertexWithUV((double)(var8 - var9), (double)(1.0F - var10), 0.0D, (double)var5, (double)var6);
//            var3.addVertexWithUV((double)(0.0F - var9), (double)(1.0F - var10), 0.0D, (double)var4, (double)var6);
//            var3.draw();
//            GL11.glPopMatrix();
//        }
//    }


}
