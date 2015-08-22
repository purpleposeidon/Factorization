package factorization.docs;

import factorization.shared.Core;

public class LocalizedWord extends TextWord {
    public LocalizedWord(String text, String hyperlink) {
        super(Core.translateThis(text), hyperlink);
    }
}
