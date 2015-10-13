package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.VerticalSpacerWord;

public class CmdVpad implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        int height = Integer.parseInt(tokenizer.getParameter("\\vpad height"));
        out.write(new VerticalSpacerWord(height));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        tokenizer.getParameter("\\vpad height");
    }
}
