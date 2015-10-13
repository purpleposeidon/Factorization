package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.VerticalSpacerWord;

public class CmdVpad extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        int height = Integer.parseInt(tokenizer.getParameter("\\vpad height"));
        out.write(new VerticalSpacerWord(height));
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        tokenizer.getParameter("\\vpad height");
    }
}
