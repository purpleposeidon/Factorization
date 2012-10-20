package factorization.api;

public enum ExoArmorRestriction {
    NONE, HEAD, CHEST, PANTS, FEET;
    
    public boolean canUse(int id) {
        if (this == NONE) { 
            return true;
        }
        return (this.ordinal() - 1) == id;
    }
}