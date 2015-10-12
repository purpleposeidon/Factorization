package factorization.truth.api;

import factorization.truth.word.Word;

public interface ITypesetter {
    /**
     * The text is run in a nested context. Proper instruction is given in factorization:doc/README.txt
     * @param text Source to typeset
     * @param link The link to pass to emitted words. May be null.
     * @param style The style to prefix words with. May not be null; use "".
     */
    void write(String text, String link, String style) throws TruthError;

    /**
     * Helper for {@link ITypesetter#write}. The link is null and the style is empty.
     * @param text Source to typeset
     */
    void write(String text) throws TruthError;

    /**
     * Send a word directly to the typesetter. The contents of the word will not be word-wrapped.
     * @param w the Word
     */
    void write(Word w);

    /**
     * Helper for {@link ITypesetter#write(Word)}. Writes a TextWord. No wordwrapping is done!;
     * @param text Text of the word
     * @param link The link
     *             NORELEASE: delete?
     */
    void write(String text, String link);
}
