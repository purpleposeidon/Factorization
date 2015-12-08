package factorization.truth.word;

import factorization.shared.Core;
import factorization.truth.WordPage;
import factorization.truth.api.IHtmlTypesetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.opengl.GL11;

public class IconWord extends Word {
    public static final int BLOCK_TEXTURE = 234, ITEM_TEXTURE = 567;
    
    private final TextureAtlasSprite icon;
    private final boolean isBlock;
    
    public IconWord(TextureAtlasSprite icon, int texture) {
        if (icon == null) icon = BlockIcons.error;
        this.icon = icon;
        isBlock = texture == BLOCK_TEXTURE;
    }

    @Override
    public int draw(int x, int y, boolean hover, FontRenderer font) {
        y -= 4;
        int width = getWidth(null);
        int height = 16;
        int z = 0;
        double u0 = icon.getMinU();
        double v0 = icon.getMinV();
        double u1 = icon.getMaxU();
        double v1 = icon.getMaxV();
        Minecraft.getMinecraft().renderEngine.bindTexture(isBlock ? Core.blockAtlas : Core.itemAtlas);
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
    public void writeHtml(IHtmlTypesetter out) {
        out.html(icon.getIconName()); // TODO?
        //final String imgPath = out.img(resource.toString());
        //out.html(String.format("<img width=%s height=%s src=\"%s\" />", width, height, imgPath));
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
    public int getWordHeight() {
        return WordPage.TEXT_HEIGHT + getPaddingAbove();
    }

}
