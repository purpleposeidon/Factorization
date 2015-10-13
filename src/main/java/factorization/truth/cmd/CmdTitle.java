package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import net.minecraft.util.EnumChatFormatting;

public class CmdTitle extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("No content");
        out.write(val, out.getInfo().link, out.getInfo().style + EnumChatFormatting.UNDERLINE + EnumChatFormatting.BOLD);
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("\n\n<h1>");
        String val = tokenizer.getParameter("title text");
        out.write(val);
        out.html("</h1>\n");
    }
}
