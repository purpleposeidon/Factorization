package factorization.colossi;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.shared.Core;

import java.io.IOException;

public class StateMachineExecutor<E extends Enum<E> & IStateMachine<E> > implements IDataSerializable {
    final ColossusController controller;
    E state;
    int age = 0;
    final String machineName;
    
    public StateMachineExecutor(ColossusController controller, String machineName, E initialState) {
        this.controller = controller;
        this.machineName = machineName + "_";
        this.state = initialState;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        age = data.as(Share.PRIVATE, machineName + prefix + "_age").putInt(age);
        state = data.as(Share.PRIVATE, machineName + prefix + "_state").putEnum(state);
        return this;
    }

    public void forceState(E nextState) {
        Awakener.msg(machineName + nextState);
        state.onExitState(controller, nextState);
        nextState.onEnterState(controller, state);
        age = 0;
        state = nextState;
    }

    public E getState() {
        return state;
    }

    void tick() {
        E nextState = state.tick(controller, age++);
        if (nextState != state) {
            forceState(nextState);
        }
    }
}
