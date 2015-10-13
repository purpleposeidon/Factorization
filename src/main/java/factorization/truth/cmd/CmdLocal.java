package factorization.truth.cmd;

import factorization.shared.Core;
import factorization.truth.api.*;
import factorization.truth.word.LocalizedWord;

public class CmdLocal implements ITypesetCommand {
    @Override
    public void callClient(IClientTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.write(new LocalizedWord(localizationKey));
    }

    @Override
    public void callHTML(IHtmlTypesetter out, ITokenizer tokenizer) throws TruthError {
        String localizationKey = tokenizer.getParameter("localization key");
        out.html(Core.translate(localizationKey));
    }
}
