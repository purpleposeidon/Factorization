package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.TextWord;
import net.minecraft.util.EnumChatFormatting;

public class CmdTitle implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String val = tokenizer.getParameter("No content");
        final TextWord v = new TextWord(val);
        v.setStyle("" + EnumChatFormatting.UNDERLINE + EnumChatFormatting.BOLD);
        out.write(v);
        // There's some dumb vanilla issue getting the width of bold text
        //out.write(val, null, );
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("\n\n<h1>");
        String val = tokenizer.getParameter("title text");
        out.write(val);
        out.html("</h1>\n");
    }
}
