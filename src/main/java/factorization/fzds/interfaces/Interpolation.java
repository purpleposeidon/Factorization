package factorization.fzds.interfaces;


public enum Interpolation {
    CONSTANT {
        @Override
        public double scale(double t) {
            if (t >= 1) return 1;
            return 0;
        }
    },
    LINEAR {
        @Override
        public double scale(double t) {
            return t;
        }
    },
    SMOOTH {
        @Override
        public double scale(double t) {
            // http://en.wikipedia.org/wiki/Smoothstep
            return t * t * (3 - 2 * t);
        }
    },
    SMOOTHER {
        @Override
        public double scale(double t) {
            // http://en.wikipedia.org/wiki/Smoothstep
            return t * t * t * (t * (t * 6 - 15) + 10);
        }
    };
    
    public abstract double scale(double t);
}
