package factorization.beauty;

import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.init.Blocks;
import org.lwjgl.opengl.GL11;

public class BlockRenderBiblioGen extends FactorizationBlockRender {
    TileEntityBiblioGenRenderer render = new TileEntityBiblioGenRenderer();
    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            rb.setRenderBoundsFromBlock(Blocks.enchanting_table);
            return rb.renderStandardBlock(Blocks.enchanting_table, x, y, z);
        } else {
            Minecraft.getMinecraft().getTextureManager().bindTexture(Core.blockAtlas);
            rb.renderBlockAsItem(Blocks.enchanting_table, 0, 1F);
            GL11.glPushMatrix();
            GL11.glRotatef(90, 0, 1, 0);
            GL11.glTranslatef(0, 4.125F / 16F, 0);
            render.drawBook();
            GL11.glPopMatrix();
            return true;
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BIBLIO_GEN;
    }
}
