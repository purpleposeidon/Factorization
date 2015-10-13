package factorization.truth.cmd;

import factorization.truth.DocumentationModule;
import factorization.truth.api.AgnosticCommand;
import factorization.truth.api.ITokenizer;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;

public class CmdInclude extends AgnosticCommand {
    @Override
    protected void call(ITypesetter out, ITokenizer tokenizer) throws TruthError {
        String name = tokenizer.getParameter("\\include{page name}");
        if (name == null) {
            throw new TruthError("No page name specified");
        }
        String subtext = DocumentationModule.readDocument(out.getDomain(), name);
        out.write(subtext);
    }
}
