package factorization.truth.cmd;

import factorization.truth.api.*;
import net.minecraft.util.EnumChatFormatting;

public class CmdStyle implements ITypesetCommand {
    EnumChatFormatting mc;
    String html;
    public CmdStyle(EnumChatFormatting chatStyle, String htmlTag) {
        mc = chatStyle;
        html = htmlTag;
    }

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String content = tokenizer.getParameter("paramter content");
        out.write(content, null, "" + mc);
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String content = tokenizer.getParameter("styled text");
        String open = "<" + html + ">";
        String close = "</" + html + ">";
        out.html(open);
        out.write(content);
        out.html(close);
    }
}
