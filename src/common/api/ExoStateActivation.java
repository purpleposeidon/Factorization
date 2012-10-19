package factorization.api;

public enum ExoStateActivation {
    OFF, FIRSTON, ON, FIRSTOFF;
    public boolean on = false;
    public boolean changing = false;
    static {
        FIRSTON.on = true;
        ON.on = true;
        FIRSTON.changing = true;
        FIRSTOFF.changing = true;
    }
}