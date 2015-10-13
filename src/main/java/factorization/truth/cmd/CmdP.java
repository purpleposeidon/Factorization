package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.WordPage;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdP extends InternalCmd {

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        WordPage p = out.getCurrentPage();
        p.nl();
        if (out.getCurrentPage() == p) {
            p.nl();
        }
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        out.html("<br>\n");
    }
}
