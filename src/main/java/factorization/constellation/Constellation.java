package factorization.constellation;

public class Constellation {
    /**
     * @param star Registers an object to get render callbacks.
     */
    public static void register(IStar star) {
        if (star.getStarPos().w.isRemote) {
            ConstellationManager.addStar(star);
        } else {
            star.setRegion(NULL_REGION);
        }
    }

    private static final IStarRegion NULL_REGION = new IStarRegion() {
        @Override
        public void dirtyStar(IStar star) {
        }

        @Override
        public void removeStar(IStar star) {
        }
    };
}
