package factorization.api;

public enum MechaStateShader {
    NORMAL, INVERSE, RISINGEDGE, FALLINGEDGE, TOGGLE, INVTOGGLE;

    public String brief() {
        switch (this) {
        default:
        case NORMAL:
            return "";
        case INVERSE:
            return "¬";
        case RISINGEDGE:
            return "/";
        case FALLINGEDGE:
            return "\\";
        case TOGGLE:
            return "┬";
            //return "⏉";
        case INVTOGGLE:
            return "┴";
            //return "⏊";
        }
    }
}
