package factorization.truth.word;

import factorization.shared.Core;
import factorization.shared.FzIcons;
import factorization.truth.WordPage;
import factorization.truth.api.IHtmlTypesetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

public class IconWord extends Word {
    private final TextureAtlasSprite icon;

    public IconWord(TextureAtlasSprite icon) {
        if (icon == null) icon = FzIcons.items$error;
        this.icon = icon;
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
        Minecraft.getMinecraft().renderEngine.bindTexture(Core.blockAtlas);
        Tessellator tessI = Tessellator.getInstance();
        WorldRenderer tess = tessI.getWorldRenderer();
        GL11.glColor4f(1, 1, 1, 1);
        tess.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        tess.tex(u0, v0).putPosition(x + 0, y + 0, z);
        tess.tex(u0, v1).putPosition(x + 0, y + height, z);
        tess.tex(u1, v1).putPosition(x + width, y + height, z);
        tess.tex(u1, v0).putPosition(x + width, y + 0, z);
        tessI.draw();
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
