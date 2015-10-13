package factorization.truth.cmd;

import factorization.truth.DocumentationModule;
import factorization.truth.api.*;

public class CmdInclude extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String name = tokenizer.getParameter("\\include{page name}");
        if (name == null) {
            throw new TruthError("No page name specified");
        }
        TypesetInfo info = out.getInfo();
        String subtext = DocumentationModule.readDocument(info.domain, name);
        out.write(subtext, info.link, info.style);
    }
}
