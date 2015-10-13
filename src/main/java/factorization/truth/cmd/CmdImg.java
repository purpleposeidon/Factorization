package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.ImgWord;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

public class CmdImg extends InternalCmd {
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
            img = new ImgWord(rl, out.getInfo().link, width, height);
        } else {
            img = new ImgWord(rl, out.getInfo().link);
            if (scaleOrWidth != null) {
                img.scale(Double.parseDouble(scaleOrWidth));
            }
        }
        return img;
    }

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ImgWord img = getImg(out, tokenizer);
        img.fitToPage(out.pageWidth, out.pageHeight);
        out.write(img);
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        ImgWord img = getImg(out, tokenizer);
        final int width = img.width;
        final int height = img.height;
        final String img1 = out.img(img.resource.toString());
        out.html(String.format("<img width=%s height=%s src=\"%s\" />", width, height, img1));
    }
}
