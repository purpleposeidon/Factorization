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
    },
    // Some interpolations from http://sol.gfxile.net/interpolation/
    SQUARE {
        @Override
        public double scale(double t) {
            return t * t;
        }
    },
    INV_SQUARE {
        @Override
        public double scale(double t) {
            t = 1 - t;
            return 1 - t * t;
        }
    },
    CUBIC {
        @Override
        public double scale(double t) {
            return t * t * t;
        }
    },
    INV_CUBIC {
        @Override
        public double scale(double t) {
            t = 1 - t;
            return 1 - t * t * t;
        }
    },
    SMOOTH2 {
        @Override
        public double scale(double t) {
            return SMOOTH.scale(SMOOTH.scale(t));
        }
    },
    SMOOTH3 {
        @Override
        public double scale(double t) {
            return SMOOTH.scale(SMOOTH.scale(SMOOTH.scale(t)));
        }
    },
    HALF_SIN {
        private static final double halfpi = Math.PI / 2.0;
        @Override
        public double scale(double t) {
            return Math.sin(t * halfpi);
        }
    },
    INV_HALF_SIN {
        private static final double halfpi = Math.PI / 2.0;
        @Override
        public double scale(double t) {
            return 1 - Math.sin(t * halfpi);
        }
    }
    ;
    
    public abstract double scale(double t);
}
