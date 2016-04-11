package factorization.charge.enet;

import com.google.common.collect.Maps;
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
    public boolean injectPower(IWorkerContext generator, WorkUnit unit) {
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

    public WireLeader getLeader(ContextTileEntity context) {
        if (context.side == null) {
            throw new NullPointerException("context.side");
        }
        Coord at = new Coord(context.te);
        MemberPos memberPos = new MemberPos(at, context.side);
        return getLeader(memberPos, at, context.side);
    }

    WireLeader getLeader(MemberPos mp, Coord at, EnumFacing dir) {
        if (at == null || at.w == null) return null;
        final Map<MemberPos, WireLeader> cache = getCache(at.w);
        WireLeader leader = cache.get(mp);
        if (leader != null) {
            return leader;
        }
        if (cache.containsKey(mp)) {
            return null;
        } //cache.clear();
        FlatFace ff = Flat.get(at, dir);
        if (ff.getSpecies() != WireCharge.SPECIES) return null;
        WireCharge wc = (WireCharge) ff;
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
        h.search();
        return h.found;
    }

    boolean inject(Coord at, EnumFacing dir) {
        WireLeader leader = getLeader(new MemberPos(at, dir), at, dir);
        if (leader == null) return false;
        return leader.injectPower(at, dir);
    }

    private Map<MemberPos, WireLeader> getCache(World w) {
        Map<MemberPos, WireLeader> cache = injectionCache.get(w);
        if (cache == null) {
            cache = Maps.newHashMap();
            injectionCache.put(w, cache);
        }
        return cache;
    }

    @Override
    public void workerAdded(IWorkerContext context, WorkUnit unit) {

    }

    @Override
    public void workerDestroyed(IWorkerContext context) {

    }

    @Override
    public void workerNeedsPower(IWorkerContext context) {
        Coord at;
        EnumFacing side;
        if (context instanceof ContextTileEntity) {
            ContextTileEntity te = (ContextTileEntity) context;
            at = new Coord(te.te);
            side = te.side;
        } else if (context instanceof ContextBlock) {
            ContextBlock block = (ContextBlock) context;
            at = block.at;
            side = block.side;
        } else {
            return;
        }
        if (side != null) {
            tryGive(context, at, side);
        } else {
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (tryGive(context, at, dir)) {
                    return;
                }
            }
        }
    }

    boolean tryGive(IWorkerContext context, Coord at, EnumFacing side) {
        MemberPos mp = new MemberPos(at, side);
        WireLeader myLeader = getLeader(mp, at, side);
        if (myLeader == null) return false;
        if (myLeader.powerSum <= 0) return false;
        myLeader.powerSum--;
        context.give(CHARGE, false);
        return true;
    }

    int deathCount = 0;
    void dirtyCache(World w, MemberPos at) {
        Map<MemberPos, WireLeader> fc = injectionCache.get(w);
        if (fc == null) return;
        if (deathCount++ > 16) {
            // Makes mass die-off more efficient
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
