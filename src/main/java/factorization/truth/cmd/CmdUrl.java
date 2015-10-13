package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.HtmlConversionTypesetter;
import factorization.truth.word.URIWord;

public class CmdUrl extends InternalCmd {
    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String uriLink = tokenizer.getParameter("\\url missing parameter: uriLink");
        String content = tokenizer.getParameter("\\url missing parameter: content");
        out.write(new URIWord(content, uriLink));
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String url = tokenizer.getParameter("url target");
        String content = tokenizer.getParameter("link content");
        out.write(content, url, out.getInfo().style);
    }
}
