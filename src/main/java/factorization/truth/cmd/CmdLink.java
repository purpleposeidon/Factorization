package factorization.truth.cmd;

import factorization.truth.api.*;
import factorization.truth.export.ExportHtml;

public class CmdLink implements ITypesetCommand {
    final boolean isIndex;
    public CmdLink(boolean isIndex) {
        this.isIndex = isIndex;
    }

    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String newLink = tokenizer.getParameter("missing destination parameter");
        String content = tokenizer.getParameter("missing content parameter");
        out.write(content, newLink, "");
        if (isIndex) {
            out.write("\\nl");
        }
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
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
