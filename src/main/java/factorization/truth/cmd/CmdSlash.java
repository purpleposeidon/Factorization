package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.TextWord;

public class CmdSlash implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.write(new TextWord("\\"));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("\\");
    }
}
