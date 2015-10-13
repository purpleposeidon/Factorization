package factorization.truth.cmd;

import factorization.truth.WordPage;
import factorization.truth.api.*;

public class CmdDash implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ((WordPage) out.getCurrentPage()).nl();
        out.write(" - ");
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("<br> •");
    }
}
