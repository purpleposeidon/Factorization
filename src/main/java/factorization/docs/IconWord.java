package factorization.docs;

import factorization.common.BlockIcons;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;

import org.lwjgl.opengl.GL11;

import factorization.shared.Core;

public class IconWord extends Word {
    public static final int BLOCK_TEXTURE = 234, ITEM_TEXTURE = 567;
    
    private final IIcon icon;
    private final boolean isBlock;
    
    public IconWord(String hyperlink, IIcon icon, int texture) {
        super(hyperlink);
        if (icon == null) icon = BlockIcons.error;
        this.icon = icon;
        isBlock = texture == BLOCK_TEXTURE;
    }

    @Override
    public int draw(DocViewer doc, int x, int y, boolean hover) {
        y -= 4;
        int width = getWidth(null);
        int height = 16;
        int z = 0;
        double u0 = icon.getMinU();
        double v0 = icon.getMinV();
        double u1 = icon.getMaxU();
        double v1 = icon.getMaxV();
        doc.mc.renderEngine.bindTexture(isBlock ? Core.blockAtlas : Core.itemAtlas);
        Tessellator tess = new Tessellator();
        GL11.glColor4f(1, 1, 1, 1);
        tess.startDrawingQuads();
        tess.addVertexWithUV(x + 0, y + 0, z, u0, v0);
        tess.addVertexWithUV(x + 0, y + height, z, u0, v1);
        tess.addVertexWithUV(x + width, y + height, z, u1, v1);
        tess.addVertexWithUV(x + width, y + 0, z, u1, v0);
        tess.draw();
        return 16;
    }
    
    @Override
    public int getWidth(FontRenderer font) {
        return 16;
    }
    
    @Override
    public int getPaddingAbove() {
        return (16 - WordPage.TEXT_HEIGHT) / 2;
    }
    
    @Override
    public int getPaddingBelow() {
        return WordPage.TEXT_HEIGHT + getPaddingAbove();
    }

}
