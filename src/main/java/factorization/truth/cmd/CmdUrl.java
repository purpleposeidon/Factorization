package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.word.URIWord;

public class CmdUrl implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String uriLink = tokenizer.getParameter("\\url missing parameter: uriLink");
        String content = tokenizer.getParameter("\\url missing parameter: content");
        out.write(new URIWord(content, uriLink));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String url = tokenizer.getParameter("url target");
        String content = tokenizer.getParameter("link content");
        out.html("<a href=\"" + url + "\">");
        out.write(content);
        out.html("</a>");
    }
}
