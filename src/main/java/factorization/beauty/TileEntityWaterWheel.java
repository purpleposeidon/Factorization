package factorization.beauty;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.shared.*;
import factorization.util.FzUtil;
import factorization.util.NumUtil;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.BiomeGenOcean;
import net.minecraft.world.biome.BiomeGenRiver;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;

public class TileEntityWaterWheel extends TileEntityCommon implements IRotationalEnergySource, IMeterInfo {
    ForgeDirection wheelDirection = ForgeDirection.UP;
    double power_per_tick, power_this_tick, target_velocity, velocity;
    double water_strength = 0;
    boolean rs_power = false;
    int non_air_block_count;
    final EntityReference<IDeltaChunk> idcRef = new EntityReference<IDeltaChunk>();

    @Override
    public void setWorldObj(World w) {
        super.setWorldObj(w);
        idcRef.setWorld(w);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.WATER_WHEEL_GEN;
    }

    @Override
    public boolean canConnect(ForgeDirection direction) {
        return direction == this.wheelDirection.getOpposite();
    }

    @Override
    public double availableEnergy(ForgeDirection direction) {
        if (direction != wheelDirection.getOpposite()) return 0;
        return power_this_tick;
    }

    @Override
    public double takeEnergy(ForgeDirection direction, double maxPower) {
        if (direction != wheelDirection.getOpposite()) return 0;
        if (maxPower > power_this_tick) {
            maxPower = power_this_tick;
        }
        power_this_tick -= maxPower;
        return maxPower;
    }

    @Override
    public double getVelocity(ForgeDirection direction) {
        if (direction != wheelDirection.getOpposite()) return 0;
        // Except I had to reverse it! Silliness!
        int sign = 1; //SpaceUtil.sign(wheelDirection);
        if (velocity < -MAX_SPEED) return -MAX_SPEED * sign;
        if (velocity > MAX_SPEED) return MAX_SPEED * sign;
        return velocity * sign;
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        if (!willHarvest) return super.removedByPlayer(player, willHarvest);
        if (PlayerUtil.isPlayerCreative(player) && player.isSneaking()) {
            IDeltaChunk idc = idcRef.getEntity();
            if (idc != null) idc.setDead();
            return super.removedByPlayer(player, willHarvest);
        }
        if (idcRef.trackedAndAlive()) return false;
        return super.removedByPlayer(player, willHarvest);
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        wheelDirection = ForgeDirection.getOrientation(side);
        if (wheelDirection.offsetY != 0) {
            wheelDirection = ForgeDirection.getOrientation(SpaceUtil.determineFlatOrientation(player)).getOpposite();
        }
    }

    @Override
    public boolean isTileEntityInvalid() {
        return this.isInvalid();
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        wheelDirection = data.as(Share.VISIBLE, "wheelDirection").putEnum(wheelDirection);
        data.as(Share.VISIBLE, "idcRef").put(idcRef);
        power_per_tick = data.as(Share.VISIBLE, "powerPerTick").putDouble(power_per_tick);
        power_this_tick = data.as(Share.VISIBLE, "powerThisTick").putDouble(power_this_tick);
        target_velocity = data.as(Share.VISIBLE, "targetVelocity").putDouble(target_velocity);
        velocity = data.as(Share.VISIBLE, "velocity").putDouble(velocity);
        water_strength = data.as(Share.PRIVATE, "water_strength").putDouble(water_strength);
        rs_power = data.as(Share.PRIVATE, "rs_power").putBoolean(rs_power);
    }

    @Override
    public void representYoSelf() {
        super.representYoSelf();
    }

    static int channel_id = 100;
    static final int MAX_OUT = 8;
    static final int MAX_IN = 8;
    static final int MAX_RADIUS = 6;

    boolean working = false;

    @Override
    public void onNeighborTileChanged(int tilex, int tiley, int tilez) {
        neighborChanged(null);
    }

    @Override
    public void neighborChanged(Block neighbor) {
        if (working) return;
        if (idcRef.trackedAndAlive()) {
            if (getCoord().isWeaklyPowered() != rs_power) {
                rs_power = !rs_power;
                if (rs_power) {
                    target_velocity = 0;
                } else {
                    calculateWaterForce();
                }
            }
            return;
        }
        working = true;
        try {
            trySpawnMill();
        } finally {
            working = false;
        }
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == NetworkFactorization.MessageType.MillVelocity) {
            velocity = input.readDouble();
        }
        return false;
    }

    void sendVelocity() {
        broadcastMessage(null, NetworkFactorization.MessageType.MillVelocity, velocity);
    }

    private boolean trySpawnMill() {
        Coord fenceLocation = new Coord(this);
        fenceLocation.adjust(wheelDirection);
        if (!isFenceish(fenceLocation)) return false;
        DeltaCoord idcSize = new DeltaCoord(MAX_RADIUS * 2, MAX_OUT + MAX_IN, MAX_RADIUS * 2);
        DeltaCoord offset = new DeltaCoord(MAX_RADIUS, MAX_OUT, MAX_RADIUS);
        IDeltaChunk idc = DeltaChunk.allocateSlice(worldObj, channel_id, idcSize);
        idc.permit(DeltaCapability.BLOCK_PLACE,
                DeltaCapability.BLOCK_MINE,
                DeltaCapability.INTERACT,
                DeltaCapability.ROTATE,
                DeltaCapability.DIE_WHEN_EMPTY,
                DeltaCapability.REMOVE_ALL_ENTITIES);
        idc.forbid(DeltaCapability.COLLIDE_WITH_WORLD,
                DeltaCapability.COLLIDE,
                DeltaCapability.VIOLENT_COLLISIONS,
                DeltaCapability.DRAG);
        idc.setRotationalCenterOffset(offset.toVector().addVector(0.5, 0.5, 0.5));
        final ForgeDirection normal = wheelDirection.getOpposite();
        Coord at = new Coord(this).add(wheelDirection);
        at.setAsEntityLocation(idc);
        if (normal.offsetY == 0) {
            //Vec3 up = SpaceUtil.fromDirection(ForgeDirection.UP);
            //Vec3 vnorm = SpaceUtil.fromDirection(normal);
            //Vec3 axis = up.crossProduct(vnorm);
            //Quaternion rot = Quaternion.getRotationQuaternionRadians(-Math.PI / 2, axis);
            //idc.setRotation(rot);
            idc.posY += 0.5;
        } else {
            double a = .5;
            idc.posX += wheelDirection.offsetX * a;
            idc.posY += wheelDirection.offsetY * a;
            idc.posZ += wheelDirection.offsetZ * a;
            if (normal.offsetY == 1) {
                idc.posY += 1;
            }
        }
        TransferLib.move(fenceLocation, idc.getCenter(), false /* We'll do it this way to synchronize them */, true);
        idc.setPartName("Waterwheel");
        fenceLocation.setAir();
        worldObj.spawnEntityInWorld(idc);
        idcRef.trackEntity(idc);
        return true;
    }

    private boolean isFenceish(Coord fenceLocation) {
        final Block block = fenceLocation.getBlock();
        if (block instanceof BlockFence) return true;
        if (block.getRenderType() == Blocks.fence.getRenderType()) return true;
        if (block instanceof BlockWall) return true;
        if (block.getRenderType() == Blocks.cobblestone_wall.getRenderType()) return true;
        if (block instanceof BlockLog) return true;
        if (block.getMaterial() == Material.iron) return true;
        return false;
    }

    @Override
    public boolean canUpdate() {
        return FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER;
    }

    @Override
    public void updateEntity() {
        if (worldObj.isRemote) return;
        if (!idcRef.entityFound()) {
            if (velocity != 0) {
                velocity = 0;
                sendVelocity();
            }
            idcRef.getEntity();
            return;
        }
        power_this_tick = power_per_tick;
        if (velocity != target_velocity) {
            double error = target_velocity > velocity ? (velocity / target_velocity) : 1;
            velocity = (velocity * 95 + target_velocity) / 100;
            power_this_tick *= error;
            if (error > 0.9999) {
                velocity = target_velocity;
            }
            IDeltaChunk idc = idcRef.getEntity();
            if (idc != null) {
                idc.setRotationalVelocity(Quaternion.getRotationQuaternionRadians(getVelocity(wheelDirection.getOpposite()), wheelDirection));
            }
            sendVelocity();
        }
        if (worldObj.getTotalWorldTime() % 200 != 0) return;
        calculateWaterForce();
    }

    static double V_SCALE = 0.025; // Scales down velocity (also doesn't change power output)
    static double WATER_POWER_SCALE = 1.0 / 50.0; // Boosts the power output (and does not influence velocity)
    static double riverFlow = 1.0 / (Math.sqrt(2) * 8); // Water in river biome is considered to have a 'flow' of riverFlow * Vec3(1, 0, 1); same power as diagonally flowing water
    static double oceanFlow = riverFlow / 8; // And a similar case for oceans
    static double otherFlowNerf = 1.0 / 4.0;
    static double MAX_SPEED = Math.min(1.0 / (Math.sqrt(2) * 2) / 64, IRotationalEnergySource.MAX_SPEED / 64); // Maximum velocity (doesn't change power output)
    static int sea_level_range_min = -4; // non-flowing blocks within river/ocean biomes, but outside of this range, do not flow
    static int sea_level_range_max = +2; // The range is relative to sealevel ('worldProvider.getAverageGroundLevel'). Sealevel is 64 for normal worlds, 4 for superflat.


    void updatePowerPerTick() {
        power_per_tick = Math.abs(water_strength);
        target_velocity = water_strength * V_SCALE;
        if (target_velocity > MAX_SPEED) {
            target_velocity = MAX_SPEED;
        } else if (target_velocity < -MAX_SPEED) {
            target_velocity = -MAX_SPEED;
        }
        power_per_tick *= WATER_POWER_SCALE;
    }

    @Override
    public String getInfo() {
        if (!idcRef.trackedAndAlive()) {
            return "No waterhweel";
        }
        String speed;
        if (velocity == 0) {
            speed = "stopped";
        } else {
            double spr = 1 / (Math.toDegrees(velocity) * 20 / 360);
            if (spr > 60) {
                speed = (int) spr + " SPR";
            } else {
                speed = FzUtil.toRpm(velocity);
            }
        }
        return "Water power: " + (int) Math.abs(water_strength * 10) +
                "\nSpeed: " + speed;
    }

    void calculateWaterForce() {
        final IDeltaChunk idc = idcRef.getEntity();
        if (idc == null || getCoord().isWeaklyPowered()) {
            water_strength = 0;
            updatePowerPerTick();
            return;
        }
        non_air_block_count = 0;
        // Overworld: Require river biome + sealevel or flowing blocks, & wood
        // Nether: Require flowing lava & metal blocks
        final Vec3 tmp = SpaceUtil.newVec();
        final int sea_min = worldObj.provider.getAverageGroundLevel() + sea_level_range_min;
        final int sea_max = worldObj.provider.getAverageGroundLevel() + sea_level_range_max;
        final Vec3 water_torque = SpaceUtil.newVec();
        final Vec3 centerOfMass = idc.getCenter().toMiddleVector();
        ForgeDirection a = this.wheelDirection;
        if (SpaceUtil.sign(a) == -1) a = a.getOpposite();
        final DeltaCoord fwd = new DeltaCoord(wheelDirection).incrScale(3);
        final Vec3 mask = SpaceUtil.fromDirection(a);
        final Vec3 antiMask = SpaceUtil.fromDirection(a.getOpposite());
        SpaceUtil.incrAdd(antiMask, Vec3.createVectorHelper(1, 1, 1));

        ICoordFunction measure = new ICoordFunction() {
            boolean waterOkay(Coord here, Block hereBlock) {
                if (hereBlock.getMaterial() != Material.wood) return false;
                if (hereBlock.isNormalCube()) {
                    // Stairs & slabs & fences can pass fine; if you're solid tho, you've got to be exposed
                    byte exposedSides = 0;
                    for (Coord n : here.getNeighborsAdjacent()) {
                        if (!n.isAir()) exposedSides++;
                    }
                    if (exposedSides <= 4) return false;
                }
                return true;
            }

            boolean lavaOkay(Coord here, Block hereBlock) {
                return hereBlock.getMaterial() == Material.iron;
            }

            @Override
            public void handle(Coord here) {
                final Block hereBlock = here.getBlock();
                if (!hereBlock.isAir(here.w, here.x, here.y, here.z)) {
                    non_air_block_count++;
                } else {
                    return;
                }
                boolean waterOkay = waterOkay(here, hereBlock);
                boolean lavaOkay = lavaOkay(here, hereBlock);
                if (!waterOkay && !lavaOkay) return;

                Coord real = idc.shadow2realCoordPrecise(here);
                Block realBlock = real.getBlock();
                tmp.xCoord = tmp.yCoord = tmp.zCoord = 0;
                if (waterOkay && realBlock.getMaterial() == Material.water) {
                    if (realBlock.getMobilityFlag() == 0) return;
                    realBlock.velocityToAddToEntity(worldObj, real.x, real.y, real.z, null, tmp);
                    if (tmp.xCoord == 0 && tmp.yCoord == 0 && tmp.zCoord == 0) {
                        BiomeGenBase biome = real.getBiome();
                        if (NumUtil.intersect(sea_min, sea_max, real.y, real.y)) {
                            for (int forward = 0; forward < 1; forward++) {
                                if (biome instanceof BiomeGenRiver) {
                                    tmp.xCoord = riverFlow;
                                    tmp.zCoord = riverFlow;
                                } else if (biome instanceof BiomeGenOcean) {
                                    tmp.xCoord = oceanFlow;
                                    tmp.zCoord = oceanFlow;
                                } else {
                                    biome = real.add(fwd).getBiome();
                                    continue;
                                }
                                break;
                            }
                        }
                    } else {
                        SpaceUtil.incrScale(tmp, otherFlowNerf);
                    }
                } else if (lavaOkay && realBlock.getMaterial() == Material.lava) {
                    realBlock.velocityToAddToEntity(worldObj, real.x, real.y, real.z, null, tmp);
                } else {
                    return;
                }
                idc.getRotation().applyReverseRotation(tmp);
                Vec3 P = SpaceUtil.incrSubtract(here.toMiddleVector(), centerOfMass);
                SpaceUtil.incrComponentMultiply(P, antiMask); // Remove the axial component of P
                SpaceUtil.incrComponentMultiply(tmp, antiMask); // And same for F
                Vec3 torque = tmp.crossProduct(P);

                SpaceUtil.incrAdd(water_torque, torque);
            }
        };
        Coord.iterateCube(idc.getCorner(), idc.getFarCorner(), measure);
        water_strength = SpaceUtil.sum(water_torque) * -SpaceUtil.sign(wheelDirection);

        updatePowerPerTick();
    }

    @Override
    public IIcon getIcon(ForgeDirection dir) {
        return BlockIcons.beauty$water_side;
    }
}
