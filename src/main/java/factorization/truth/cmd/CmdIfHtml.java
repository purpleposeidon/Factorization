package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdIfHtml extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        tokenizer.getParameter("true branch");
        String falseBranch = tokenizer.getParameter("false branch");
        out.write(falseBranch);
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String trueBranch = tokenizer.getParameter("true branch");
        tokenizer.getParameter("false branch");
        out.write(trueBranch);
    }
}
