package factorization.charge.enet;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.api.energy.*;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatCoord;
import factorization.flat.api.FlatFace;
import factorization.shared.Core;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

public class ChargeEnetSubsys implements IEnergyNet {
    public static final ChargeEnetSubsys instance = new ChargeEnetSubsys();

    public static final ResourceLocation FZ_CHARGE = new ResourceLocation("factorization:charge");
    public static final WorkUnit CHARGE = WorkUnit.get(EnergyCategory.ELECTRIC, FZ_CHARGE);
    public WireCharge wire0;
    public WireLeader wireLeader;

    public void setup() {
        Core.loadBus(this);
        IEnergyNet.register(this);
        wire0 = new WireCharge();
        Flat.registerStatic(new ResourceLocation("factorization:charge/wire"), wire0);
        wireLeader = new WireLeader();
        Flat.registerDynamic(new ResourceLocation("factorization:charge/wire_leader"), wireLeader);
    }

    @Override
    public boolean canHandlePower(WorkUnit unit) {
        return CHARGE.equals(unit);
    }

    @Override
    public boolean injectPower(IContext generator, WorkUnit unit) {
        if (unit != CHARGE) return false;
        Coord at = null;
        EnumFacing dir;
        if (generator instanceof ContextTileEntity) {
            ContextTileEntity cte = (ContextTileEntity) generator;
            at = new Coord(cte.te);
            dir = cte.side;
        } else if (generator instanceof ContextBlock) {
            ContextBlock ctb = (ContextBlock) generator;
            at = ctb.at;
            dir = ctb.side;
        } else {
            return false;
        }
        if (dir != null) {
            return inject(at, dir);
        }
        for (EnumFacing d : EnumFacing.VALUES) {
            if (inject(at, d)) {
                return true;
            }
        }
        return false;
    }

    boolean inject(Coord at, EnumFacing dir) {
        FlatFace ff = Flat.get(at, dir);
        if (ff.getSpecies() != WireCharge.SPECIES) return false;
        WireCharge wc = (WireCharge) ff;
        final MemberPos mp = new MemberPos(at, dir);
        final Map<MemberPos, WireLeader> cache = getCache(at);
        WireLeader leader = cache.get(mp);
        if (leader == null) {
            class Holder extends WireCharge.LeaderSearch {
                WireLeader found = null;
                public Holder(WireCharge me, Coord at, EnumFacing side) {
                    super(me, at, side);
                }

                @Override
                boolean onFound(WireLeader leader, FlatCoord input) {
                    cache.put(mp, leader);
                    found = leader;
                    return true;
                }
            }
            Holder h = new Holder(wc, at, dir);
            leader = h.found;
        }
        if (leader== null) return false;
        return leader.injectPower();
    }

    private Map<MemberPos, WireLeader> getCache(Coord at) {
        Map<MemberPos, WireLeader> cache = injectionCache.get(at.w);
        if (cache == null) {
            cache = Maps.newHashMap();
            injectionCache.put(at.w, cache);
        }
        return cache;
    }

    @Override
    public void workerAdded(IContext context, WorkUnit unit) {

    }

    @Override
    public void workerDestroyed(IContext context) {

    }

    @Override
    public void workerNeedsPower(IContext context) {

    }

    int deathCount = 0;
    void dirtyCache(World w, MemberPos at) {
        Map<MemberPos, WireLeader> fc = injectionCache.get(w);
        if (fc == null) return;
        if (deathCount++ > 16) {
            // Makes math die-off more efficient
            fc.clear();
            return;
        }
        fc.remove(at);
    }


    final WeakHashMap<World, Map<MemberPos, WireLeader>> injectionCache = new WeakHashMap<World, Map<MemberPos, WireLeader>>();
    final Map<World, List<FlatCoord>> tickList = Maps.newConcurrentMap();
    public void registerLeader(FlatCoord at) {
        List<FlatCoord> list = tickList.get(at.at.w);
        if (list == null) {
            list = FastBag.create();
            tickList.put(at.at.w, list);
        }
        list.add(at);
    }

    @SubscribeEvent
    public void tick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.world.isRemote) return;
        List<FlatCoord> tickables = tickList.get(event.world);
        if (tickables == null) return;
        for (Iterator<FlatCoord> iter = tickables.iterator(); iter.hasNext(); ) {
            FlatCoord fc = iter.next();
            if (!fc.exists()) {
                iter.remove();
                continue;
            }
            FlatFace face = fc.get();
            if (face.getSpecies() != WireLeader.SPECIES || !(face instanceof WireLeader)) {
                iter.remove();
                continue;
            }
            ((WireLeader) face).tick(fc);
        }
        if (event.world.getTotalWorldTime() % (20 * 60 * 5) == 0) {
            Map<MemberPos, WireLeader> l = injectionCache.get(event.world);
            if (l != null) {
                l.clear();
            }
        }
        deathCount = 0;
    }
}
