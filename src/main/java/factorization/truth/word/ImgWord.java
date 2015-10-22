package factorization.truth.word;

import factorization.truth.api.IHtmlTypesetter;
import factorization.util.FzUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ImgWord extends Word {
    public final ResourceLocation resource;
    public int width = 12, height = 12;
    public ImgWord(ResourceLocation resource) {
        this.resource = resource;
        autosize();
    }

    public ImgWord(ResourceLocation resource, int width, int height) {
        this(resource);
        this.width = width;
        this.height = height;
    }

    public void scale(double scale) {
        width *= scale;
        height *= scale;
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
    public int getPaddingBelow() {
        return height - getPaddingAbove();
    }

    @Override
    public int draw(int x, int y, boolean hover, FontRenderer font) {
        int z = 0;
        Minecraft.getMinecraft().renderEngine.bindTexture(resource); // memleak goes here! :|
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

    private static final HashMap<ResourceLocation, Pair<Integer, Integer>> size_cache = new HashMap<ResourceLocation, Pair<Integer, Integer>>();

    private void autosize() {
        Pair<Integer, Integer> cached = size_cache.get(resource);
        if (cached != null) {
            width = cached.getLeft();
            height = cached.getRight();
            return;
        }

        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        IResource iresource = null;
        InputStream is = null;
        try {
            iresource = resourceManager.getResource(resource);
            is = iresource.getInputStream();
            BufferedImage bufferedimage = ImageIO.read(is);
            this.width = bufferedimage.getWidth();
            this.height = bufferedimage.getHeight();
            size_cache.put(resource, Pair.of(width, height));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            FzUtil.closeNoisily("reading size of image", is);
        }
    }

    public void fitToPage(int pageWidth, int pageHeight) {
        double s = 1.0;
        if (width > pageWidth) {
            s = pageWidth / (double) width;
        }
        if (height > pageHeight) {
            double h = pageHeight / (double) height;
            if (h < s) {
                s = h;
            }
        }
        scale(s);
    }

    @Override
    public void writeHtml(IHtmlTypesetter out) {
        final String imgPath = out.img(resource.toString());
        out.html(String.format("<img width=%s height=%s src=\"%s\" />", width, height, imgPath));
    }
}
