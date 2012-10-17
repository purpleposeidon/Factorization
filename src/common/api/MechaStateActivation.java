package factorization.api;

public enum MechaStateActivation {
    OFF, FIRSTON, ON;
    
    public boolean isOn() {
        return this != OFF;
    }
}