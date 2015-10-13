package factorization.truth.cmd;

import factorization.truth.WordPage;
import factorization.truth.api.*;
import net.minecraft.util.EnumChatFormatting;

public class CmdHeader implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("No header title");
        out.write(val, null, "" + EnumChatFormatting.BOLD);
        ((WordPage) out.getCurrentPage()).nl();
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("header text");
        out.html("<h2>");
        out.write(val);
        out.html("</h2>\n");
    }
}
