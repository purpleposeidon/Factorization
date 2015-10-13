package factorization.truth.word;

import factorization.shared.Core;

public class LocalizedWord extends TextWord {
    public LocalizedWord(String text) {
        super(Core.translateThis(text));
    }
}
