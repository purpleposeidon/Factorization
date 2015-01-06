package factorization.docs;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

public class ImgWord extends Word {
    final ResourceLocation resource;
    int width = 12, height = 12;
    public ImgWord(ResourceLocation resource, String hyperlink) {
        super(hyperlink);
        this.resource = resource;
    }
    
    public ImgWord(ResourceLocation resource, String hyperlink, int width, int height) {
        this(resource, hyperlink);
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return resource + " ==> " + getLink();
    }
    
    @Override
    public int getWidth(FontRenderer font) {
        return width;
    }

    @Override
    public int draw(DocViewer doc, int x, int y, boolean hover) {
        int z = 0;
        doc.mc.renderEngine.bindTexture(resource);
        Tessellator tess = new Tessellator();
        GL11.glColor4f(1, 1, 1, 1);
        tess.startDrawingQuads();
        tess.addVertexWithUV(x + 0, y + 0, z, 0, 0);
        tess.addVertexWithUV(x + 0, y + height, z, 0, 1);
        tess.addVertexWithUV(x + width, y + height, z, 1, 1);
        tess.addVertexWithUV(x + width, y + 0, z, 1, 0);
        tess.draw();
        return 16;
    }

}
