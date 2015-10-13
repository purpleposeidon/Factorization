package factorization.truth.cmd;

import factorization.truth.WordPage;
import factorization.truth.api.*;

public class CmdP implements ITypesetCommand {

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        WordPage p = (WordPage) out.getCurrentPage();
        p.nl();
        if (out.getCurrentPage() == p) {
            p.nl();
        }
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("<br>\n");
    }
}
