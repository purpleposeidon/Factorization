package factorization.api.wind;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public interface IWindModel {
    /**
     * @param mill The windmill making the query, which should not obstruct itself. May be null.
     * @return the wind power at the location. The 'default' value is Vec3(-1, 0, 0), since vanilla clouds blow westward.
     * (What are the units? Probably some ratio of IC2's 'MCW', which are, uhm...)
     * The model may or may not take obstruction from other windmills into account; it's totally optional.
     */
    Vec3 getWindPower(World w, BlockPos pos, IWindmill mill);

    /**
     * Windmills should register themselves with the wind model whenever they are loaded or placed.
     * The wind model is not expected to track windmills across world loads.
     */
    <T extends TileEntity & IWindmill> void registerWindmillTileEntity(T mill);

    /**
     * When a windmill is removed/dies, it should call this method.
     * If the windmodel needs to respond to windmills being unloaded, then it will have to subscribe to chunk unload events.
     */
    <T extends TileEntity & IWindmill> void deregisterWindmillTileEntity(T mill);

    /**
     * Entity equivalent of registerWindmillTileEntity
     */
    <T extends Entity & IWindmill> void registerWindmillEntity(T mill);

    /**
     * Entity equivalent of deregisterWindmillTileEntity
     */
    <T extends Entity & IWindmill> void deregisterWindmillEntity(T mill);
}
