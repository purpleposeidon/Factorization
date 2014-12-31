package factorization.colossi;

interface IStateMachine<E extends Enum> {
    E tick(ColossusController controller, int age);
    void onEnterState(ColossusController controller, E prevState);
    void onExitState(ColossusController controller, E nextState);
}
