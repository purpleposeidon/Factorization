package factorization.colossi;

import java.io.IOException;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;

public class ColossusAI implements IDataSerializable {
    final ColossusController controller;

    AIState state = AIState.IDLE;
    int age = 0;
    
    public ColossusAI(ColossusController controller) {
        this.controller = controller;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        state = data.as(Share.PRIVATE, prefix + "_state").putEnum(state);
        return this;
    }
    
    void tick() {
        AIState nextState = state.tick(controller, age++);
        if (nextState != state) {
            state.onExitState(controller, nextState);
            nextState.onEnterState(controller, state);
            age = 0;
            state = nextState;
            System.out.println("Current state: " + nextState);
        }
    }
}
