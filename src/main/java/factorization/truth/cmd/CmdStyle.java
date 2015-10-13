package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import net.minecraft.util.EnumChatFormatting;

public class CmdStyle extends InternalCmd {
    EnumChatFormatting mc;
    String html;
    public CmdStyle(EnumChatFormatting chatStyle, String htmlTag) {
        mc = chatStyle;
        html = htmlTag;
    }

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String content = tokenizer.getParameter("paramter content");
        out.write(content, out.getInfo().link, out.getInfo().style + mc);
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String content = tokenizer.getParameter("styled text");
        String open = "<" + html + ">";
        String close = "</" + html + ">";
        out.html(open);
        out.write(content);
        out.html(close);
    }
}
