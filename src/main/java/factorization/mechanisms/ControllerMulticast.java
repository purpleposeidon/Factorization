package factorization.mechanisms;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.util.SpaceUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Allows multiple controllers on an IDC. Nicely sets down the IDC when it has no more controllers.
 */
public class ControllerMulticast implements IDCController {

    /**
     * Call this to join/create the listener set. Do not use this method after deserialization.
     *
     * @param idc        The IDC to listen to
     * @param constraint The controller to add.
     * @throws IllegalArgumentException if the IDC already has a non-multicast controller.
     */
    public static void register(IDeltaChunk idc, IDCController constraint) {
        rejoin(idc, constraint);
        changeCount(idc, +1);
    }

    /**
     * Since the controllers aren't serialized, this is the mechanism to remind the IDC who the controllers are. Use with IDCRef.
     *
     * Best used with autoJoin.
     *
     * @param idc        The IDC to remind who is controller it
     * @param constraint The controller
     * @throws IllegalArgumentException if the IDC already has a non-multicast controller.
     *
     */
    public static void rejoin(IDeltaChunk idc, IDCController constraint) {
        IDCController controller = idc.getController();
        if (controller == IDCController.default_controller) {
            idc.setController(controller = new ControllerMulticast());
        } else if (!(controller instanceof ControllerMulticast)) {
            throw new IllegalArgumentException("IDC already had a controller, and it is not a ControllerMulticast! IDC: " + idc + "; controller: " + controller + "; constraint: " + constraint);
        }
        ControllerMulticast sys = (ControllerMulticast) controller;
        sys.addConstraint(constraint);
    }

    /**
     * Remove the controller from the multicast set. The IDC will be dropped if there are no more controllers.
     *
     * @param idc        The IDC who is getting the controller removed
     * @param constraint The controller to remove.
     */
    public static void deregister(IDeltaChunk idc, IDCController constraint) {
        IDCController controller = idc.getController();
        if (!(controller instanceof ControllerMulticast)) {
            Core.logWarning("Tried to deregister constraint for IDC that isn't a ControllerMulticast! IDC: " + idc + "; controller: " + controller + "; constraint: ", constraint);
            return;
        }
        ControllerMulticast sys = (ControllerMulticast) controller;
        sys.removeConstraint(constraint);
        if (changeCount(idc, -1) <= 0) {
            dropIDC(idc);
        }
    }

    /**
     * @param idc The IDC to check
     * @return true if the IDC's controller is a ControllerMulticast, or it can have one applied.
     */
    public static boolean usable(IDeltaChunk idc) {
        IDCController controller = idc.getController();
        return controller == IDCController.default_controller || controller instanceof ControllerMulticast;
    }

    public static IDCController[] getControllers(IDeltaChunk idc) {
        IDCController controller = idc.getController();
        if (controller instanceof ControllerMulticast) {
            ControllerMulticast cm = (ControllerMulticast) controller;
            return cm.constraints;
        }
        return new IDCController[0];
    }

    private static int changeCount(IDeltaChunk idc, int d) {
        NBTTagCompound tag = idc.getEntityData();
        String key = "KinematicSystemEntries";
        int old = tag.getInteger(key);
        int upd = old + d;
        if (upd < 0) upd = 0;
        tag.setInteger(key, upd);
        return upd;
    }

    private static void dropIDC(final IDeltaChunk idc) {
        // TODO: check all forge directions (including UNKNOWN) to count how many clashes there are. Move to the minimal direction before dropping IF the # of clashes is > 10% of the block count
        idc.setRotation(new Quaternion());
        idc.setRotationalVelocity(new Quaternion());
        final Coord min = idc.getCorner();
        final Coord max = idc.getFarCorner();
        final Coord real = new Coord(idc);
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord shadow) {
                if (shadow.isAir()) return;
                real.set(shadow);
                idc.shadow2real(real);
                real.x--;
                real.y--;
                real.z--;
                if (real.isReplacable()) {
                    TransferLib.move(shadow, real, true, true);
                    real.markBlockForUpdate();
                } else {
                    shadow.breakBlock();
                }
            }
        });
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                here.setAir();
            }
        });
        idc.setDead();
    }

    public static void push(IDeltaChunk idc, Coord at, Vec3 force) {
        for (IDCController constraint : ControllerMulticast.getControllers(idc)) {
            if (constraint instanceof TileEntityHinge) { // Sound design!
                TileEntityHinge hinge = (TileEntityHinge) constraint;
                if (hinge.getCoord().isWeaklyPowered()) return;
                hinge.applyForce(idc, at, force);
                return;
            }
        }

        double mass = MassCalculator.calculateMass(idc);
        /* Whereupon St. Isaac Newton did set down the Holy Law of Nature, that
         * the sum of the forces upon a body is equal to the mass of the body times
         * the acceleration of the body, and whereupon we have calculated the mass
         * of the body, let us therefor grant unto our hookedIdc an IMPULSE OF VELOCITY
         * equal to the force multiplied by the inverse of the mass.
         */
        SpaceUtil.incrScale(force, 1 / mass);
        Vec3 newVel = SpaceUtil.add(force, SpaceUtil.fromEntVel(idc));
        SpaceUtil.toEntVel(idc, newVel);
    }

    private IDCController[] constraints = new IDCController[0];

    void addConstraint(IDCController constraint) {
        if (ArrayUtils.contains(constraints, constraint)) return;
        constraints = ArrayUtils.add(constraints, constraint);
        if (constraint instanceof TileEntityHinge) {
            // Or could use Arrays.sort
            // This is fine so long as there's only 1 hinge
            int i = constraints.length - 1;
            IDCController first = constraints[0];
            constraints[0] = constraint;
            constraints[i] = first;
        }
    }

    void removeConstraint(IDCController constraint) {
        constraints = ArrayUtils.removeElement(constraints, constraint);
    }

    @Override
    public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) {
        for (IDCController c : constraints) {
            if (c.placeBlock(idc, player, at)) return true;
        }
        return false;
    }

    @Override
    public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        for (IDCController c : constraints) {
            if (c.breakBlock(idc, player, at, sideHit)) return true;
        }
        return false;
    }

    @Override
    public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        for (IDCController c : constraints) {
            if (c.hitBlock(idc, player, at, sideHit)) return true;
        }
        return false;
    }

    @Override
    public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) {
        for (IDCController c : constraints) {
            if (c.useBlock(idc, player, at, sideHit)) return true;
        }
        return false;
    }

    @Override
    public void idcDied(IDeltaChunk idc) {
        for (IDCController c : constraints) c.idcDied(idc);
    }

    @Override
    public void beforeUpdate(IDeltaChunk idc) {
        for (IDCController c : constraints) c.beforeUpdate(idc);
    }

    @Override
    public void afterUpdate(IDeltaChunk idc) {
        for (IDCController c : constraints) c.afterUpdate(idc);
    }

    /**
     * Returns an EntityReference that will call ControllerMulticast.rejoin for you.
     * {@code
     *     final EntityReference<IDeltaChunk> idcRef = ControllerMulticast.autoJoin(this);
     * }
     * @param controller The controller that you want to rejoin.
     * @return The reference
     */
    public static EntityReference<IDeltaChunk> autoJoin(final IDCController controller) {
        EntityReference<IDeltaChunk> ret = new EntityReference<IDeltaChunk>();
        ret.whenFound(new EntityReference.OnFound<IDeltaChunk>() {
            @Override
            public void found(IDeltaChunk ent) {
                ControllerMulticast.rejoin(ent, controller);
            }
        });
        return ret;
    }
}
