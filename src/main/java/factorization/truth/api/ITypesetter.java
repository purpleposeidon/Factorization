package factorization.truth.api;

import factorization.truth.word.Word;

/**
 * NYI.
 */
public interface ITypesetter {
    boolean isClient();
    boolean isHtml();

    void emitHtml(String text);
    void emitWord(Word word);
    void emit(String text);

    void process(String text);
}
