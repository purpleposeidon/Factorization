package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.Word;

import java.util.ArrayList;

public class CmdSegStart extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        ArrayList<ArrayList<Word>> lines = out.getCurrentPage().text;
        if (!lines.isEmpty()) {
            out.segmentStart = lines.get(lines.size() - 1);
        }
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        // NOP
    }
}
