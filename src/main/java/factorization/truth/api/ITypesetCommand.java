package factorization.truth.api;

import factorization.truth.TypesetCommands;

/**
 * @see TypesetCommands
 */
public interface ITypesetCommand {
    void handle(ITypesetter out);
}
