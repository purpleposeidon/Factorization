package factorization.common;

import cpw.mods.fml.common.registry.GameRegistry;
import factorization.common.servo.TileEntityServoRail;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

public enum FactoryType {
    ROUTER(0, true, TileEntityRouter.class, "factory_router"), // Send/retrieve items from connected inventories
    //1 -- This used to be CUTTER
    //2 -- This used to be MAKER
    STAMPER(3, true, TileEntityStamper.class, "factory_stamper"), // Crafts craft packets, and outputs results
    //4 -- This used to be QUEUE
    BARREL(5, false, TileEntityBarrel.class, "factory_barrel"), // Store huge quantities of identical items
    LAMP(6, false, TileEntityWrathLamp.class, "factory_lamp"), //spawn a bunch of AIR blocks around and below
    //7 -- this was the BlockDarkIron, which got moved.
    PACKAGER(8, true, STAMPER.gui, TileEntityPackager.class, "factory_packager"), //crafts its input as a 3x3 or 2x2
    //9 -- This used to be SENTRYDEMON
    WRATHFIRE(10, false, TileEntityWrathFire.class, "factory_fire"), //burn things
    SLAGFURNACE(11, true, TileEntitySlagFurnace.class, "factory_slag"), //get extra ore output
    BATTERY(12, false, TileEntityBattery.class, "factory_battery"),
    //13 -- This used to be SOLARTURBINE
    LEADWIRE(14, false, TileEntityWire.class, "factory_solder"),
    HEATER(15, false, TileEntityHeater.class, "factory_heater"), //work furnaces without fuel
    MIRROR(16, false, TileEntityMirror.class, "factory_mirror"), //reflect sunlight onto IReflectionTargets
    GRINDER(17, true, TileEntityGrinder.class, "factory_grinder"), //grind
    MIXER(18, true, TileEntityMixer.class, "factory_mixer"), //crafts its input as shapeless recipes of 2-4 ingredients
    CRYSTALLIZER(19, true, TileEntityCrystallizer.class, "factory_crystal"), //grow metallic crystals
    //20 -- Used to be GREENWARE
    STEAMTURBINE(21, false, TileEntitySteamTurbine.class, "factory_steamturbine"), //A generic steam turbine; works with other mods' steam
    SOLARBOILER(22, false, TileEntitySolarBoiler.class, "factory_solarfurnace"), //Produces steam from sunlight
    ROCKETENGINE(23, false, TileEntityRocketEngine.class, "factory_rocketengine"), //Is a rocket
    EXTENDED(24, false, TileEntityExtension.class, "factory_ext"), //Used for multipiece blocks (like beds & rocket engines)
    CERAMIC(25, false, TileEntityGreenware.class, "factory_ceramic"), //clay sculpture (Not really implemented)
    LEYDENJAR(26, false, TileEntityLeydenJar.class, "factory_leyjar"), //inefficient bulk energy storage
    SERVORAIL(27, false, TileEntityServoRail.class, "factory_rail"),
    PARASIEVE(28, true, TileEntityParaSieve.class, "factory_sieve"),
    COMPRESSIONCRAFTER(29, false, TileEntityCompressionCrafter.class, "factory_compact"),
    

    POCKETCRAFTGUI(101, true)
    ;
    
    static {
        //CERAMIC.disable();
    }

    final public int md;
    final public int gui;
    final public boolean hasGui;
    final private Class clazz;
    final public String te_id;
    private TileEntityCommon representative;
    private boolean can_represent = true;
    private boolean disabled = false;

    public TileEntityCommon getRepresentative() {
        if (!can_represent) {
            return null;
        }
        if (representative == null) {
            if (can_represent) {
                can_represent = TileEntityCommon.class.isAssignableFrom(clazz);
                if (!can_represent) {
                    return null;
                }
            }
            try {
                representative = ((Class<? extends TileEntityCommon>)clazz).newInstance();
            } catch (Throwable e) {
                throw new IllegalArgumentException(e);
            }
            MinecraftForge.EVENT_BUS.register(representative);
        }
        return representative;
    }

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

    TileEntityCommon makeTileEntity() {
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

    public static FactoryType fromMd(int md) {
        if (md < 0) {
            return null;
        }
        if (md >= mapper.mapping.length) {
            return null;
        }
        return mapper.mapping[md];
    }

    ItemStack itemStack() {
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
            GameRegistry.registerTileEntity(ft.clazz, ft.te_id);
        }
    }

    public boolean connectRedstone() {
        return this == ROUTER || this == STAMPER || this == PACKAGER;
    }
}
