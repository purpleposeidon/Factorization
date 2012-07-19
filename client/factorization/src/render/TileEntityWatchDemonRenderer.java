package factorization.src.render;

import net.minecraft.src.EntityItem;
import net.minecraft.src.ItemStack;
import net.minecraft.src.RenderItem;
import net.minecraft.src.RenderManager;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;
import net.minecraft.src.TileEntitySpecialRenderer;
import net.minecraft.src.forge.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

import factorization.src.Core;

public class TileEntityWatchDemonRenderer extends TileEntitySpecialRenderer {
	EntityItem entity = new EntityItem(null);
	RenderItem renderItem;

	public TileEntityWatchDemonRenderer() {
		entity.item = new ItemStack(Core.registry.bound_tiny_demon);
	}

	@Override
	public void renderTileEntityAt(TileEntity te, double x, double y, double z,
			float partial) {

		renderItem = (RenderItem) RenderManager.instance.getEntityClassRenderObject(EntityItem.class);
		MinecraftForgeClient.bindTexture(Core.texture_file_item);
		//renderItem.loadTexture(FactorizationCore.texture_file_item);
		GL11.glPushMatrix();
		GL11.glTranslatef((float) x + 0.5F, (float) y + 0.1F, (float) z + 0.5F);
		GL11.glRotatef(180.0F - RenderManager.instance.playerViewY, 0.0F, 1.0F, 0.0F);
		GL11.glScalef(0.5F, 0.5F, 0.5F);
		renderItem(Core.registry.bound_tiny_demon.getIconFromDamage(0));
		GL11.glPopMatrix();
		MinecraftForgeClient.unbindTexture();
	}

	private void renderItem(int textureIndex) {
		Tessellator var3 = Tessellator.instance;
		float var4 = (float) (textureIndex % 16 * 16 + 0) / 256.0F;
		float var5 = (float) (textureIndex % 16 * 16 + 16) / 256.0F;
		float var6 = (float) (textureIndex / 16 * 16 + 0) / 256.0F;
		float var7 = (float) (textureIndex / 16 * 16 + 16) / 256.0F;
		float var8 = 1.0F;
		float var9 = 0.5F;
		float var10 = 0.25F;

		GL11.glPushMatrix();

		var3.startDrawingQuads();
		var3.setNormal(0.0F, 1.0F, 0.0F);
		var3.addVertexWithUV((double) (0.0F - var9), (double) (0.0F - var10), 0.0D,
				(double) var4, (double) var7);
		var3.addVertexWithUV((double) (var8 - var9), (double) (0.0F - var10), 0.0D,
				(double) var5, (double) var7);
		var3.addVertexWithUV((double) (var8 - var9), (double) (1.0F - var10), 0.0D,
				(double) var5, (double) var6);
		var3.addVertexWithUV((double) (0.0F - var9), (double) (1.0F - var10), 0.0D,
				(double) var4, (double) var6);
		var3.draw();
		GL11.glPopMatrix();
	}

}
