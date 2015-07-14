package factorization.common;

import cpw.mods.fml.common.registry.GameRegistry;
import factorization.astro.TileEntityRocketEngine;
import factorization.beauty.*;
import factorization.ceramics.TileEntityGreenware;
import factorization.charge.*;
import factorization.crafting.TileEntityCompressionCrafter;
import factorization.crafting.TileEntityMixer;
import factorization.crafting.TileEntityPackager;
import factorization.crafting.TileEntityStamper;
import factorization.fzds.DeltaChunk;
import factorization.mechanics.SocketPoweredCrank;
import factorization.mechanics.TileEntityHinge;
import factorization.oreprocessing.TileEntityCrystallizer;
import factorization.oreprocessing.TileEntitySlagFurnace;
import factorization.servo.TileEntityParaSieve;
import factorization.servo.TileEntityServoRail;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.shared.TileEntityExtension;
import factorization.shared.TileEntityFactorization;
import factorization.sockets.*;
import factorization.sockets.fanturpeller.BlowEntities;
import factorization.sockets.fanturpeller.PumpLiquids;
import factorization.weird.TileEntityDayBarrel;
import factorization.wrath.TileEntityWrathLamp;
import net.minecraft.item.ItemStack;

public enum FactoryType {
    //Traced here is the history of Factorization.
    //0 -- This used to be ROUTER
    //1 -- This used to be CUTTER
    //2 -- This used to be MAKER
    STAMPER(3, true, TileEntityStamper.class, "factory_stamper"), // Crafts a single item
    //4 -- This used to be QUEUE
    //5 -- This used to be BARREL
    LAMP(6, false, TileEntityWrathLamp.class, "factory_lamp"), //spawn a bunch of AIR blocks around and below
    //7 -- this was the BlockDarkIron, which got moved.
    PACKAGER(8, true, STAMPER.gui, TileEntityPackager.class, "factory_packager"), //crafts its input as a 3x3 or 2x2
    //9 -- This used to be SENTRYDEMON
    //10 -- This used to be WRATHFIRE
    SLAGFURNACE(11, true, TileEntitySlagFurnace.class, "factory_slag"), //get extra ore output
    BATTERY(12, false, TileEntityBattery.class, "factory_battery"),
    //13 -- This used to be SOLARTURBINE
    LEADWIRE(14, false, TileEntityWire.class, "factory_solder"),
    HEATER(15, false, TileEntityHeater.class, "factory_heater"), //work furnaces without fuel
    MIRROR(16, false, TileEntityMirror.class, "factory_mirror"), //reflect sunlight onto IReflectionTargets
    //17 -- This used to be GRINDER
    MIXER(18, true, TileEntityMixer.class, "factory_mixer"), //crafts its input as shapeless recipes of 2-4 ingredients
    CRYSTALLIZER(19, true, TileEntityCrystallizer.class, "factory_crystal"), //grow metallic crystals
    //20 -- Used to be GREENWARE
    STEAMTURBINE(21, false, TileEntitySteamTurbine.class, "factory_steamturbine"), //A generic steam turbine; works with other mods' steam
    SOLARBOILER(22, false, TileEntitySolarBoiler.class, "factory_solarfurnace"), //Produces steam from sunlight
    ROCKETENGINE(23, false, TileEntityRocketEngine.class, "factory_rocketengine"), //Is a rocket
    EXTENDED(24, false, TileEntityExtension.class, "factory_ext"), //Used for multipiece blocks (like beds & rocket engines)
    CERAMIC(25, false, TileEntityGreenware.class, "factory_ceramic"), //clay sculpture
    LEYDENJAR(26, false, TileEntityLeydenJar.class, "factory_leyjar"), //inefficient bulk energy storage
    SERVORAIL(27, false, TileEntityServoRail.class, "factory_rail"),
    PARASIEVE(28, true, TileEntityParaSieve.class, "factory_sieve"),
    COMPRESSIONCRAFTER(29, false, TileEntityCompressionCrafter.class, "factory_compact"),
    DAYBARREL(30, false, TileEntityDayBarrel.class, "factory_barrel2"),
    CALIOMETRIC_BURNER(31, false, TileEntityCaliometricBurner.class, "factory_calory"),
    SOCKET_EMPTY(32, false, SocketEmpty.class, "fzsock_empty"),
    SOCKET_LACERATOR(33, false, SocketLacerator.class, "fzsock_lacerate"),
    SOCKET_ROBOTHAND(34, false, SocketRobotHand.class, "fzsock_hand"),
    SOCKET_SHIFTER(35, true, SocketShifter.class, "fzsock_shift"),
    //36 -- Was the short-lived SOCKET_FANTURPELLER; which is now abstract
    SOCKET_PUMP(37, false, PumpLiquids.class, "fzsock_pump"),
    //38 -- Was the short-lived SOCKET_POWERGEN
    SOCKET_BLOWER(39, true, BlowEntities.class, "fzsock_blow"),
    //40 -- Was the short-lived SOCKET_MIXER
    SOCKET_BARE_MOTOR(41, false, SocketBareMotor.class, "fzsock_motor"),
    SOCKET_SCISSORS(42, false, SocketScissors.class, "fzsock_scissors"),
    CREATIVE_CHARGE(43, false, InfiniteChargeBlock.class, "factory_creative_charge"),
    HINGE(44, false, TileEntityHinge.class, "factory_hinge"),
    //ANCHOR(45, false, TileEntityAnchor.class, "factory_anchor"),
    //SOCKET_HAND_CRANK(46, false, SocketHandCrank.class, "fzsock_hand_crank"),
    SOCKET_POWERED_CRANK(47, false, SocketPoweredCrank.class, "factory_powered_crank"),
    //SOCKET_CONTROLLED_CRANK(48, true, SocketControlledCrank.class, "factory_controlled_crank"),
    SAP_TAP(49, false, TileEntitySapTap.class, "factory_sap_extractor"),
    ANTHRO_GEN(50, false, TileEntityAnthroGen.class, "factory_anthro_gen"),
    BIBLIO_GEN(51, false, TileEntityBiblioGen.class, "factory_biblio_gen"),
    SHAFT_GEN(52, false, TileEntityShaftGen.class, "factory_shaft_gen"),
    STEAM_SHAFT(53, false, TileEntitySteamShaft.class, "factory_steam_shaft"),

    POCKETCRAFTGUI(101, true),
    NONE(102, false)
    ;

    public static int MAX_ID = 0;
    static {
        for (FactoryType ft : values()) {
            MAX_ID = Math.max(MAX_ID, ft.md);
        }
        if (!FzConfig.enable_rocketry) {
            ROCKETENGINE.disable();
        }
        if (!DeltaChunk.enabled()) {
            HINGE.disable();
            //ANCHOR.disable();
            ROCKETENGINE.disable();
        }
    }
    
    final public int md;
    final public int gui;
    final public boolean hasGui;
    final private Class<? extends TileEntityCommon> clazz;
    final public String te_id;
    private TileEntityCommon representative;
    private boolean can_represent = true;
    private boolean disabled = false;

    public TileEntityCommon getRepresentative() {
        if (!can_represent || disabled) {
            return null;
        }
        if (representative == null) {
            if (clazz == null) {
                can_represent = false;
                return null;
            }
            if (can_represent) {
                can_represent = TileEntityCommon.class.isAssignableFrom(clazz);
                if (!can_represent) {
                    return null;
                }
            }
            try {
                representative = ((Class<? extends TileEntityCommon>)clazz).newInstance();
            } catch (Throwable e) {
                throw new IllegalArgumentException("Can not instantiate: " + toString(), e);
            }
            representative.representYoSelf();
        }
        return representative;
    }
    
    public Class<? extends TileEntityCommon> getFactoryTypeClass() { return clazz; }

    static class mapper {
        //bluh java
        static FactoryType mapping[] = new FactoryType[128];
    }

    FactoryType(int metadata, boolean use_gui, int gui_id, Class clazz, String name) {
        md = metadata;
        if (use_gui) {
            gui = gui_id;
        } else {
            gui = -1;
        }
        hasGui = use_gui;
        assert mapper.mapping[md] == null;
        mapper.mapping[md] = this;
        this.clazz = clazz;
        this.te_id = name;
        TileEntityCommon rep = null;
        representative = rep;
    }

    FactoryType(int md, boolean use_gui, Class clazz, String name) {
        this(md, use_gui, md, clazz, name);
    }

    FactoryType(int md, boolean use_gui) {
        this(md, use_gui, md, null, null);
    }
    
    void disable() {
        disabled = true;
    }

    public TileEntityCommon makeTileEntity() {
        if (clazz == null) {
            Core.logWarning("Note: " + this + " is a FactoryType with no associated TE");
            return null;
        }
        try {
            return (TileEntityCommon) clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isInstance(TileEntityFactorization ent) {
        if (ent == null) {
            return false;
        }
        return ent.getFactoryType() == this;
    }

    public boolean is(int md) {
        return md == this.md;
    }

    public static FactoryType fromMd(short md) {
        if (md < 0) {
            return null;
        }
        if (md >= mapper.mapping.length) {
            return null;
        }
        return mapper.mapping[md];
    }

    public ItemStack itemStack() {
        if (disabled) {
            return null;
        }
        ItemStack ret = new ItemStack(Core.registry.item_factorization, 1, this.md);
        return ret;
    }

    public static void registerTileEntities() {
        for (FactoryType ft : FactoryType.values()) {
            if (ft.clazz == null || ft.te_id == null) {
                continue;
            }
            if (ft == SOCKET_LACERATOR) {
                GameRegistry.registerTileEntityWithAlternatives(ft.clazz, ft.te_id, "factory_grinder");
            } else if (ft == SOCKET_BLOWER) {
                GameRegistry.registerTileEntityWithAlternatives(ft.clazz, ft.te_id, "fzsock_fanturpeller");
            } else {
                GameRegistry.registerTileEntity(ft.clazz, ft.te_id);
            }
        }
    }

    public boolean connectRedstone() {
        return this == STAMPER || this == PACKAGER;
    }
    
    public ItemStack asSocketItem() {
        return new ItemStack(Core.registry.socket_part, 1, md);
    }
}
