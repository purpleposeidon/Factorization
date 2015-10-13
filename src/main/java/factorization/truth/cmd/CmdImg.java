package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.ImgWord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class CmdImg implements ITypesetCommand {
    ImgWord getImg(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String imgName = tokenizer.getParameter("domain:path/to/image.png");
        String scaleOrWidth = tokenizer.getOptionalParameter();
        String heightS = tokenizer.getOptionalParameter();

        ResourceLocation rl = new ResourceLocation(imgName);
        try {
            Minecraft mc = Minecraft.getMinecraft();
            IResource r = mc.getResourceManager().getResource(rl);
            if (r == null) {
                throw new TruthError("Not found: " + imgName);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new TruthError(e.getMessage());
        }

        ImgWord img;
        if (heightS != null) {
            int width = Integer.parseInt(scaleOrWidth);
            int height = Integer.parseInt(heightS);
            img = new ImgWord(rl, width, height);
        } else {
            img = new ImgWord(rl);
            if (scaleOrWidth != null) {
                img.scale(Double.parseDouble(scaleOrWidth));
            }
        }
        return img;
    }

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ImgWord img = getImg(out, tokenizer);
        img.fitToPage(out.getPageWidth(), out.getPageHeight());
        out.write(img);
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        ImgWord img = getImg(out, tokenizer);
        final int width = img.width;
        final int height = img.height;
        final String img1 = out.img(img.resource.toString());
        out.html(String.format("<img width=%s height=%s src=\"%s\" />", width, height, img1));
    }
}
