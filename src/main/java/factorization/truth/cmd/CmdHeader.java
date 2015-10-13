package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import net.minecraft.util.EnumChatFormatting;

public class CmdHeader extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("No header title");
        out.write(val, out.getInfo().link, out.getInfo().style + EnumChatFormatting.BOLD);
        out.getCurrentPage().nl();
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("header text");
        out.html("<h2>");
        out.write(val);
        out.html("</h2>\n");
    }
}
