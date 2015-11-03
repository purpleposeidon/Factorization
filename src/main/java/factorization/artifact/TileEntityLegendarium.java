package factorization.artifact;

import com.google.common.base.Strings;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.FzConfig;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.Sound;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import factorization.weird.poster.EntityPoster;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSign;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import java.io.IOException;
import java.util.*;

public class TileEntityLegendarium extends TileEntityCommon {
    static final int MIN_SIZE = FzConfig.legendarium_queue_size; // The queue must be this size before something can be removed
    static final int POSTER_RANGE = 16;
    static final float MIN_DAMAGE = 0.10F;
    static final int DAMAGE_PAD = 24;
    static final int SIGN_RANGE = 1;

    private static final boolean DEBUG = Core.dev_environ;

    private static final int WAIT_TIME = DEBUG ? 4 * 1000 : FzConfig.legendarium_delay_hours * 60 * 60 * 1000;

    long last_insert_time = 0;
    ArrayList<ItemStack> queue = new ArrayList<ItemStack>();

    @Override
    public void putData(DataHelper data) throws IOException {
        last_insert_time = data.as(Share.PRIVATE, "lastInsertTime").putLong(last_insert_time);
        queue = data.as(Share.PRIVATE, "queue").putItemList(queue);
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Lamp;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.LEGENDARIUM;
    }

    boolean isDroid(EntityPlayer player) {
        if (player instanceof FakePlayer) {
            new Notice(this, "factorization.legendarium.nodroidsallowed").sendToAll();
            return true;
        }
        return false;
    }

    static boolean isTool(ItemStack is) {
        if (is == null) return false;
        if (is.stackSize != 1) return false;
        if (is.getMaxStackSize() > 1) return false;
        if (!is.getItem().isRepairable()) return false;
        if (is.getMaxDamage() <= 1) return false;
        if (is.getHasSubtypes()) return false;
        return true;
    }

    static String analyzeItem(ItemStack is) {
        if (is == null) return "noitem";
        if (!isTool(is)) return "not_tool";
        if (!is.hasDisplayName()) return "not_artifact";
        if (!is.isItemEnchanted()) return "not_artifact";
        final int actualDamage = (is.getMaxDamage() - is.getItemDamage());
        final float maxDamage = is.getMaxDamage() * MIN_DAMAGE + DAMAGE_PAD;
        if (actualDamage > maxDamage) return "not_broken";
        return null;
    }

    void sound(String name) {
        Sound.legendariumInsert.playAt(this);
        //worldObj.playSound(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, "factorization:legendarium_insert", 1, 1, false);
    }

    long getWaitTicks() {
        long now = System.currentTimeMillis();
        long to_wait = last_insert_time + WAIT_TIME - now;
        return (to_wait /* ms */ / 1000) /* s */ * 20 /* ticks */;
    }

    boolean isOverfull() {
        return queue.size() > MIN_SIZE;
    }

    @Override
    public boolean activate(EntityPlayer player, ForgeDirection side) {
        // Store an item
        if (worldObj.isRemote) return true;
        if (isDroid(player)) return false;
        ItemStack held = player.getHeldItem();
        if (held == null) {
            if (isOverfull()) {
                ItemStack front = queue.get(0);
                new Notice(this, "factorization.legendarium.canremove")
                        .withStyle(Style.DRAWITEM)
                        .withItem(front)
                        .sendTo(player);
                return true;
            }
            long ticks = getWaitTicks();
            if (ticks > 0) {
                new Notice(this, "factorization.legendarium.wait", FzUtil.unitTranslateTimeTicks(ticks, 2)).sendTo(player);
            } else {
                new Notice(this, "factorization.legendarium.caninsert").sendTo(player);
            }
            return true;
        }

        String analysis = analyzeItem(held);
        if (analysis != null) {
            if (analysis.equals("not_broken") && Core.dev_environ && player.isSprinting()) {
                new Notice(this, "<breaking item>").sendTo(player);
                held.setItemDamage(held.getMaxDamage() - 1);
                return true;
            }
            new Notice(this, "factorization.legendarium.item_analysis." + analysis).sendTo(player);
            return true;
        }


        long ticks = getWaitTicks();
        if (ticks > 0) {
            new Notice(this, "factorization.legendarium.wait", FzUtil.unitTranslateTimeTicks(ticks, 2)).sendTo(player);
            return true;
        }
        last_insert_time = System.currentTimeMillis();
        queue.add(ItemBrokenArtifact.build(held));
        player.setCurrentItemOrArmor(0, null);
        markDirty();
        sound("insert");
        populatePosters();
        return true;
    }

    boolean canRemove() {
        return true; //return queue.size() >= MIN_SIZE;
    }

    @Override
    public void click(EntityPlayer player) {
        // Remove an item
        if (queue.isEmpty()) return;
        if (isDroid(player)) return;
        if (ItemUtil.is(player.getHeldItem(), Core.registry.spawnPoster)) {
            if (cleanPosters() == 0) {
                populatePosters();
                new Notice(this, "factorization.legendarium.posters.populated").sendTo(player);
            } else {
                new Notice(this, "factorization.legendarium.posters.cleaned").sendTo(player);
            }
            return;
        }
        if (!isOverfull()) {
            new Notice(this, "factorization.legendarium.notfull").sendTo(player);
            return;
        }
        final ItemStack artifact = queue.remove(0);
        ItemUtil.giveItem(player, new Coord(this), artifact, ForgeDirection.UNKNOWN);
        markDirty();
        //sound("remove");
        populatePosters();
    }

    static final String legendariumCount = "legendariumCount";
    public static class LegendariumPopulation extends WorldSavedData {
        NBTTagCompound data = new NBTTagCompound();

        public LegendariumPopulation(String name) {
            super(name);
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            data = tag;
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
            for (String key : (Iterable<String>) data.func_150296_c()) {
                tag.setInteger(key, data.getInteger(key));
            }
        }

        private static String getName(World world) {
            final IChunkProvider chunkGenerator = world.provider.createChunkGenerator();
            return chunkGenerator.getClass().getName();
        }

        String isFree(World world) {
            String name = getName(world);
            return data.getString(getName(world));
        }

        void setOccupied(Coord src, EntityPlayer user, boolean v) {
            String who = "someone";
            if (user != null) who = user.getCommandSenderName();
            String worldName = getName(src.w);
            if (v) {
                data.setString(worldName, src.toShortString());
                Core.logInfo(who + " placed the hall of legends from " + src + "; worldName=" + worldName);
            } else if (data.hasKey(worldName)) {
                data.removeTag(worldName);
                Core.logInfo(who + " removed the hall of legends from " + src + "; worldName=" + worldName);
            }
            save();
        }

        static LegendariumPopulation load() {
            World w = MinecraftServer.getServer().worldServerForDimension(0);
            LegendariumPopulation ret = (LegendariumPopulation) w.loadItemData(LegendariumPopulation.class, legendariumCount);
            if (ret == null) {
                ret = new LegendariumPopulation(legendariumCount);
            }
            return ret;
        }

        public void save() {
            World w = MinecraftServer.getServer().worldServerForDimension(0);
            w.setItemData(legendariumCount, this);
            this.setDirty(true);
            w.perWorldStorage.saveAllData();
        }
    }

    @Override
    public boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        if (c.w.isRemote) return true;
        LegendariumPopulation population = LegendariumPopulation.load();
        final String free = population.isFree(c.w);
        if (!Strings.isNullOrEmpty(free)) {
            new Notice(c, "factorization.legendarium.occupied", free).sendTo(player);
            return false;
        }
        return true;
    }

    @Override
    public void onPlacedBy(EntityPlayer player, ItemStack is, int side, float hitX, float hitY, float hitZ) {
        super.onPlacedBy(player, is, side, hitX, hitY, hitZ);
        if (worldObj.isRemote) return;
        LegendariumPopulation population = LegendariumPopulation.load();
        population.setOccupied(new Coord(this), player, true);
    }

    static ItemStack unwrap(ItemStack is) {
        ItemStack ret = ItemBrokenArtifact.get(is);
        if (ret == null) return is;
        return ret;
    }

    @Override
    protected boolean removedByPlayer(EntityPlayer player, boolean willHarvest) {
        return super.removedByPlayer(player, willHarvest);
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        cleanPosters();
        Coord at = new Coord(this);
        for (ItemStack stack : queue) {
            at.spawnItem(unwrap(stack));
        }
        queue.clear();
        LegendariumPopulation population = LegendariumPopulation.load();
        population.setOccupied(new Coord(this), null, false);
    }

    @Override
    public IIcon getIcon(ForgeDirection dir) {
        if (dir.offsetY == 0) return BlockIcons.artifact$legendarium_side;
        return BlockIcons.artifact$legendarium_top;
    }

    List<EntityPoster> getPosters() {
        Coord min = new Coord(this).add(-POSTER_RANGE, -POSTER_RANGE, -POSTER_RANGE);
        Coord max = new Coord(this).add(+POSTER_RANGE, +POSTER_RANGE, +POSTER_RANGE);
        AxisAlignedBB box = SpaceUtil.createAABB(min, max);
        List<EntityPoster> ret = worldObj.getEntitiesWithinAABB(EntityPoster.class, box);
        Collections.sort(ret, new Comparator<EntityPoster>() {
            @Override
            public int compare(EntityPoster o1, EntityPoster o2) {
                double d1 = o1.getDistanceSq(xCoord, yCoord, zCoord);
                double d2 = o2.getDistanceSq(xCoord, yCoord, zCoord);
                if (d1 > d2) return +1;
                if (d1 < d2) return -1;
                return 0;
            }
        });
        return ret;
    }

    void iterateSign(EntityPoster poster, ICoordFunction function) {
        /*Coord min = new Coord(poster).add(-SIGN_RANGE, -SIGN_RANGE, -SIGN_RANGE);
        Coord max = new Coord(poster).add(+SIGN_RANGE, +SIGN_RANGE, +SIGN_RANGE);
        Coord.iterateCube(min, max, function);*/
        for (Coord n : new Coord(poster).getNeighborsAdjacent()) {
            function.handle(n);
        }
    }

    int populatePosters() {
        int ret = 0;
        cleanPosters();
        Iterator<ItemStack> it = queue.iterator();

        for (EntityPoster poster : getPosters()) {
            if (poster.getItem() != null) continue;
            if (!it.hasNext()) break;
            final ItemStack artifact = it.next().copy();
            poster.setItem(artifact);
            poster.setLocked(true);
            poster.syncWithSpawnPacket();
            ret++;
            ICoordFunction setSign = new ICoordFunction() {
                boolean set = false;
                @Override
                public void handle(Coord here) {
                    if (set) return;
                    if (!(here.getBlock() instanceof BlockSign)) return;
                    TileEntitySign sign = here.getTE(TileEntitySign.class);
                    if (sign == null) return;
                    for (int i = 0; i < sign.signText.length; i++) {
                        if (!"".equals(sign.signText[i])) return;
                    }
                    ItemStack orig = ItemBrokenArtifact.get(artifact);
                    if (orig == null) return;

                    setSignText(sign, orig);
                    // TODO: Playername?
                    here.markBlockForUpdate();
                    set = true;
                }

                private void setSignText(TileEntitySign sign, ItemStack orig) {
                    String name = orig.getDisplayName();
                    // easy case
                    int sign_max_len = 15;
                    if (name.length() < sign_max_len) {
                        sign.signText[1] = name;
                        return;
                    }
                    name = name.replaceAll("(§.)", "");
                    // Strip colors & try again
                    if (name.length() < sign_max_len) {
                        sign.signText[1] = name;
                        return;
                    }
                    // Ahh, a tough guy, eh? We'll just see 'bout that!
                    ArrayList<String> out = new ArrayList<String>();
                    String build = "";
                    int total = 0;
                    for (String word : name.split(" ")) {
                        int l = word.length();
                        if (l + total + 1 < sign_max_len || total == 0) {
                            build += " " + word;
                            total += l + 1;
                        } else {
                            out.add(build);
                            build = word;
                            total = l;
                            if (out.size() > 4) break; // bad news: It won't fit.
                        }
                    }
                    int start = 0;
                    if (out.size() <= 2) start = 1; // And center it vertically
                    int end = Math.min(sign.signText.length, out.size());
                    for (int i = start; i < end; i++) {
                        sign.signText[i] = out.get(i);
                    }
                }
            };
            iterateSign(poster, setSign);
        }
        return ret;
    }

    int cleanPosters() {
        int ret = 0;
        for (EntityPoster poster : getPosters()) {
            if (!poster.isLocked()) continue;
            if (!ItemUtil.is(poster.getItem(), Core.registry.brokenTool)) continue;
            poster.setItem(null);
            poster.setLocked(false);
            poster.syncWithSpawnPacket();
            ret++;
            ICoordFunction clearSign = new ICoordFunction() {
                @Override
                public void handle(Coord here) {
                    if (!(here.getBlock() instanceof BlockSign)) return;
                    TileEntitySign sign = here.getTE(TileEntitySign.class);
                    if (sign == null) return;
                    for (int i = 0; i < sign.signText.length; i++) {
                        sign.signText[i] = "";
                    }
                    here.markBlockForUpdate();
                }
            };
            iterateSign(poster, clearSign);
        }
        return ret;
    }

    @Override
    public boolean power() {
        return getWaitTicks() <= 0;
    }

    @Override
    public void blockUpdateTick(Block myself) {
        super.blockUpdateTick(myself);
        scheduleTick();
    }

    void scheduleTick() {
        worldObj.scheduleBlockUpdate(xCoord, yCoord, zCoord, new Coord(this).getBlock(), (int) getWaitTicks());
    }

    @Override
    public void markDirty() {
        super.markDirty();
        scheduleTick();
        new Coord(this).w.notifyBlocksOfNeighborChange(new Coord(this).x, new Coord(this).y, new Coord(this).z, new Coord(this).getBlock());
    }
}
