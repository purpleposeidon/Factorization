package factorization.api.adapter;

public class GenericAdapter<SELF_IN, SELF_OUT> implements Adapter<SELF_IN, SELF_OUT> {
    private final Class<SELF_IN> genericClass;
    private final SELF_OUT genericInterface;

    public GenericAdapter(Class<SELF_IN> genericClass, SELF_OUT genericInterface) {
        this.genericClass = genericClass;
        this.genericInterface = genericInterface;
    }

    @Override
    public SELF_OUT adapt(SELF_IN val) {
        return genericInterface;
    }

    @Override
    public boolean canAdapt(Class<?> valClass) {
        return genericClass.isAssignableFrom(valClass);
    }

    @Override
    public int priority() {
        return 1;
    }
}
