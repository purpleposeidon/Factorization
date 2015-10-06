package factorization.scrap;

public interface IRevertible {
    /**
     * Perform the action
     */
    void apply();

    /**
     * Undo the action
     */
    void revert();

    /**
     * @return a human-readable description of the action
     */
    String info();
}
