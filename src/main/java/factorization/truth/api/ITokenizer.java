package factorization.truth.api;

public interface ITokenizer {
    /**
     * Reads a parameter
     * @param info Information on the parameter, to be shown if an error occurs.
     * @return The text of the parameter. Does not return null. May return the empty string.
     * @throws TruthError if there was no parameter
     */
    String getParameter(String info) throws TruthError;

    /**
     * @return either null or the text of a following parameter.
     */
    String getOptionalParameter();
}
