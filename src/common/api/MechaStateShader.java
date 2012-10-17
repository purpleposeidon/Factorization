package factorization.api;

public enum MechaStateShader {
    NORMAL, INVERSE, RISINGEDGE;
    
    public boolean apply(MechaStateActivation msa) {
        switch (this) {
        default:
        case NORMAL: return msa.isOn();
        case INVERSE: return !msa.isOn();
        case RISINGEDGE: return msa == MechaStateActivation.FIRSTON;
        }
    }
}
