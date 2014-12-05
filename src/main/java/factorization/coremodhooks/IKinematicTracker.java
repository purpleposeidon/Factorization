package factorization.coremodhooks;

public interface IKinematicTracker {

    public double getKinematics_motX();

    public double getKinematics_motY();

    public double getKinematics_motZ();

    public double getKinematics_yaw();

    public void reset(long now);

}