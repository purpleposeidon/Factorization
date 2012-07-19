package factorization.src.render;

import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraft.src.TileEntity;

import org.lwjgl.opengl.GL11;

import factorization.src.BlockFactorization;
import factorization.src.Core;
import factorization.src.FactoryType;
import factorization.src.IFactoryType;
import factorization.src.Texture;

public class FactorizationRender {
	static Block metal = Block.obsidian;
	static Block glass = Block.glowStone;

	static boolean world_mode;
	static int x, y, z;

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

	static public boolean renderWorldBlock(RenderBlocks renderBlocks, IBlockAccess world, int x,
			int y,
			int z, Block block, int render_type) {

		FactorizationRender.renderInWorld(x, y, z);
		int md = world.getBlockMetadata(x, y, z);
		if (block == Core.registry.factory_block) {
			TileEntity te = world.getBlockTileEntity(x, y, z);
			if (te instanceof IFactoryType) {
				md = ((IFactoryType) te).getFactoryType().md;
			}
			else {
				md = -1;
			}
			if (FactoryType.LAMP.is(md)) {
				//TODO: Pick a side for the handle to go on
				FactorizationRender.renderLamp(renderBlocks, 0);
			}
			else if (FactoryType.SENTRYDEMON.is(md)) {
				FactorizationRender.renderSentryDemon(renderBlocks);
			}
			else {
				FactorizationRender.renderNormalBlock(renderBlocks, x, y, z, md);
			}
			return true;
		}
		if (block == Core.registry.lightair_block) {
			if (md == Core.registry.lightair_block.air_md) {
				return false;
			}
			if (md == Core.registry.lightair_block.fire_md) {
				FactorizationRender.renderFire(renderBlocks);
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
			}
			else {
				FactorizationRender.renderNormalBlock(renderBlocks, 0, 0, 0, damage);
			}
		}
	}

}
