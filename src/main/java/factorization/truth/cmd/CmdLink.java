package factorization.truth.cmd;

import factorization.truth.ClientTypesetter;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.TruthError;
import factorization.truth.export.ExportHtml;
import factorization.truth.export.HtmlConversionTypesetter;

public class CmdLink extends InternalCmd {
    final boolean isIndex;
    public CmdLink(boolean isIndex) {
        this.isIndex = isIndex;
    }

    @Override
    protected void callClient(ClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String newLink = tokenizer.getParameter("missing destination parameter");
        String content = tokenizer.getParameter("missing content parameter");
        out.write(content, newLink, out.getInfo().style);
        if (isIndex) {
            out.getCurrentPage().nl();
        }
    }

    @Override
    protected void callHtml(HtmlConversionTypesetter out, ITokenizer tokenizer) throws TruthError {
        String newLink = tokenizer.getParameter("missing link destination parameter");
        String content = tokenizer.getParameter("missing link content parameter");
        String ref = newLink;
        if (!newLink.contains("://")) {
            ref = out.getRoot() + newLink + ".html";
        }
        out.html("<a href=\"" + ref + "\">");
        out.write(content);
        out.html("</a>");
        if (isIndex) {
            out.html("<br>\n");
        }
        ExportHtml.visitLink(newLink);
    }
}
