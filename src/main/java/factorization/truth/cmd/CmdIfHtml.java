package factorization.truth.cmd;

import factorization.truth.api.*;

public class CmdIfHtml implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        tokenizer.getParameter("true branch");
        String falseBranch = tokenizer.getParameter("false branch");
        out.write(falseBranch);
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String trueBranch = tokenizer.getParameter("true branch");
        tokenizer.getParameter("false branch");
        out.write(trueBranch);
    }
}
