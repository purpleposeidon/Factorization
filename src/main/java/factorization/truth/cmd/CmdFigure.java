package factorization.truth.cmd;

import factorization.truth.DocWorld;
import factorization.truth.DocumentationModule;
import factorization.truth.FigurePage;
import factorization.truth.api.*;

public class CmdFigure implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        DocWorld figure = null;
        try {
            String fig = tokenizer.getParameter("Encoded document figure");
            figure = DocumentationModule.loadWorld(fig);
        } catch (Throwable t) {
            if (t instanceof TruthError) throw (TruthError) t;
            t.printStackTrace();
            throw new TruthError("figure is corrupt; see console");
        }
        if (figure == null) {
            throw new TruthError("figure failed to load");
        }
        out.addPage(new FigurePage(figure));
        // TODO: It'd be better to use a Word for this.
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        // Gosh, that'd be interesting.
        tokenizer.getParameter("encoded figure");
    }
}
