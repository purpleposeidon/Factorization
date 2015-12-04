package factorization.fzds;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;

import factorization.util.LangUtil;
import factorization.util.PlayerUtil;
import factorization.util.SpaceUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.DimensionManager;
import net.minecraft.util.EnumFacing;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.fzds.DeltaChunk.AreaMap;
import factorization.fzds.DeltaChunk.DseDestination;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.fzds.interfaces.Interpolation;
import factorization.notify.Notice;
import factorization.shared.Core;

public class FZDSCommand extends CommandBase {
    //private static DimensionSliceEntity currentWE = null;
    @Override
    public String getCommandName() {
        return "fzds";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
    
    public static abstract class SubCommand {
        String[] help;
        ArrayList<String> altNames = new ArrayList<String>();
        private boolean needOp;
        private boolean needPlayer;
        private boolean needCoord;
        private boolean needSelection;
        private boolean needCreative;
        Requires[] reqs;
        
        
        private static Splitter pipe = Splitter.on('|');
        public SubCommand(String... help) {
            this.help = help;
            if (help.length == 0) {
                throw new IllegalArgumentException("No subcommand name");
            }
            for (String s : pipe.split(help[0])) {
                altNames.add(s);
            }
        }
        
        public SubCommand() { }
        
        static ServerConfigurationManager manager = MinecraftServer.getServer().getConfigurationManager();
        
        String arg0;
        ICommandSender sender;
        EntityPlayerMP player;
        World world;
        Coord user;
        boolean op;
        boolean creative;
        IDeltaChunk selected;
        
        /** args: the arguments, not including the name */
        abstract void call(String[] args);
        
        private void reset() {
            sender = null;
            player = null;
            world = null;
            user = null;
            op = false;
            creative = false;
            selected = null;
        }
        
        private void setup(ICommandSender sender) {
            this.sender = sender;
            if (sender instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) sender;
            }
            if (sender instanceof TileEntity) {
                user = new Coord((TileEntity) sender);
            }
            selected = currentSelection.get();
            if (sender.canCommandSenderUseCommand(4, "stop")) {
                op = true;
            }
            if (sender instanceof TileEntityCommandBlock) {
                op = true;
            }
            if (op) {
                creative = true;
            }
        }
        
        DSTeleporter getTp() {
            DSTeleporter tp = new DSTeleporter((WorldServer) player.worldObj);
            tp.destination = new Coord(player.worldObj, 0, 0, 0);
            return tp;
        }
        
        boolean appropriate() {
            if (needOp && !op) {
                return false;
            }
            if (needCreative && !creative) {
                return false;
            }
            return true;
        }
        
        String details() {
            return null;
        }
        
        final String getHelp() {
            String msg = "";
            boolean first = true;
            for (String m : help) {
                if (first) {
                    first = false;
                } else {
                    msg += " ";
                }
                msg += m;
            }
            return msg;
        }
        
        final String getNeeds() {
            if (reqs.length == 0) {
                return "";
            }
            String ret = "[Need:";
            for (Requires r : reqs) {
                ret += " " + r;
            }
            return ret + "]";
        }
        
        void inform() {
            String msg = getHelp();
            String d = details();
            if (d != null) {
                msg += ": " + d;
            }
            sendChat(msg);
        }
        
        void sendChat(String msg) {
            LangUtil.sendChatMessage(true, sender, msg);
        }
    }
    
    public static enum Requires {
        OP, PLAYER, COORD, SLICE_SELECTED, CREATIVE;
        
        void apply(SubCommand sc) {
            switch (this) {
            case OP: sc.needOp = true; break;
            case PLAYER: sc.needPlayer = true; break;
            case COORD: sc.needCoord = true; break;
            case SLICE_SELECTED: sc.needSelection = true; break;
            case CREATIVE: sc.needCreative = true; break;
            }
        }
    }
    
    private static ArrayList<SubCommand> subCommands = new ArrayList<SubCommand>();
    public static SubCommand help;
    
    public static SubCommand add(SubCommand cmd, Requires... reqs) {
        for (Requires r : reqs) {
            r.apply(cmd);
        }
        cmd.reqs = reqs;
        subCommands.add(cmd);
        return cmd;
    }
    
    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        //processCommand(icommandsender, new String[0]);
        return "/fzds subcommand";
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            boolean op = PlayerUtil.isPlayerOpped(player);
            boolean cr = player.capabilities.isCreativeMode;
            if (!(op || cr)) {
                LangUtil.sendChatMessage(true, sender, "You must be op or in creative mode to use these commands");
                return;
            }
        }
        if (args.length == 0) {
            runSubCommand(help, sender, new String[] {"help"});
            return;
        }
        String cmd = args[0];
        for (SubCommand sc : subCommands) {
            for (String alias : sc.altNames) {
                if (alias.equalsIgnoreCase(cmd)) {
                    runSubCommand(sc, sender, args);
                    return;
                }
            }
        }
        LangUtil.sendChatMessage(true, sender, "Not a command");
    }
    
    private static WeakReference<IDeltaChunk> currentSelection = new WeakReference<IDeltaChunk>(null);
    
    public static void setSelection(IDeltaChunk dse) {
        currentSelection = new WeakReference<IDeltaChunk>(dse);
    }
    
    public static Coord parseCoord(World world, String src) {
        ArrayList<Integer> parts = new ArrayList<Integer>();
        for (String part : comma.split(src)) {
            parts.add(Integer.parseInt(part));
        }
        if (parts.size() == 4) {
            world = DimensionManager.getWorld(parts.remove(0));
        }
        if (world == null) {
            throw new WrongUsageException("No world specified");
        }
        return new Coord(world, parts.get(0), parts.get(1), parts.get(2));
    }
    
    private static Splitter comma = Splitter.on(",");
    
    private static World visitedWorld;
    void visitWorld(World w) {
        if (visitedWorld == null) {
            visitedWorld = w;
        } else if (visitedWorld != w) {
            throw new CommandException("References to different dimensions");
        }
    }
    
    void runSubCommand(SubCommand cmd, ICommandSender sender, String[] args) {
        cmd.reset();
        cmd.setup(sender);
        ArrayList<String> cleanedArgs = new ArrayList<String>();
        visitedWorld = null;
        boolean first = true;
        for (String a : args) {
            if (Strings.isNullOrEmpty(a)) {
                continue;
            } else if (a.startsWith("$")) {
                //set the player
                if (!cmd.op) {
                    throw new CommandException("You are not allowed to use arbitrary players");
                }
                cmd.player = MinecraftServer.getServer().getConfigurationManager().func_152612_a(a.substring(1));
                if (cmd.player == null) {
                    throw new CommandException("Player not found");
                }
                visitWorld(cmd.player.worldObj);
            } else if (a.startsWith("#")) {
                cmd.user = parseCoord(sender.getEntityWorld(), a.substring(1));
                visitWorld(cmd.user.w);
            } else if (a.startsWith("@") && first == false && !a.startsWith("@?") && !a.equals("@")) {
                String name = a.substring(1);
                Coord replace = positionVariables.get(name);
                if (replace == null) {
                    throw new CommandException("Undefined position variable: " + a);
                }
                visitWorld(replace.w);
                String r = "" + replace.x + "," + replace.y + "," + replace.z;
                cleanedArgs.add(r);
            } else {
                cleanedArgs.add(a);
            }
            first = false;
        }
        if (cmd.player != null) {
            cmd.user = new Coord(cmd.player);
        }
        if (cmd.user != null) {
            cmd.world = cmd.user.w;
        }
        if (cmd.needPlayer && cmd.player == null) {
            throw new CommandException("No player specified");
        }
        if (cmd.needCoord && cmd.user == null) {
            throw new CommandException("No coordinate specified");
        }
        if (cmd.needOp && cmd.op == false && !Core.dev_environ) {
            throw new CommandException("Insufficient permissions");
        }
        if (cmd.needSelection && cmd.selected == null) {
            throw new CommandException("No DSE selected");
        }
        cmd.arg0 = cleanedArgs.remove(0);
        try {
            String[] sc = new String[cleanedArgs.size()];
            cmd.call(cleanedArgs.toArray(sc));
        } finally {
            cmd.reset();
        }
    }
    
    static String join(ArrayList<SubCommand> cmd) {
        String ret = " ";
        boolean first = true;
        for (SubCommand sc : cmd) {
            if (!first) {
                ret += "\n";
            }
            first = false;
            ret += EnumChatFormatting.GREEN;
            if (sc.help.length == 1) {
                ret += sc.help[0];
            } else {
                ret += sc.help[0];
                for (int i = 1; i < sc.help.length; i++) {
                    ret += " " + sc.help[i];
                }
            }
            ret += EnumChatFormatting.RESET + ": " + sc.details();
        }
        return ret;
    }
    
    static HashMap<String, Coord> positionVariables = new HashMap<String, Coord>(); //NOTE: This keeps references to worlds. Oh well.
    
    static {
        help = add(new SubCommand("help", "[subcmd]+") {
            @Override
            String details() { return "Gives a list of all subcommands, or information about the given subcommands"; }
            @Override
            void call(String[] args) {
                boolean any = false;
                outer: for (String s : args) {
                    any = true;
                    for (SubCommand sc : subCommands) {
                        for (String alt : sc.altNames) {
                            if (alt.equalsIgnoreCase(s)) {
                                sc.arg0 = s;
                                sc.sender = sender;
                                sc.inform();
                                if (sc != this) {
                                    sc.reset();
                                }
                                break outer;
                            }
                        }
                    }
                }
                if (any) {
                    return;
                }
                ArrayList<SubCommand> good = new ArrayList<FZDSCommand.SubCommand>();
                for (SubCommand sc : subCommands) {
                    if (sc != this) {
                        sc.setup(sender);
                    }
                    if (sc.appropriate()) {
                        good.add(sc);
                    }
                    if (sc != this) {
                        sc.reset();
                    }
                }
                for (String msg : join(good).split("\n")) {
                    sendChat(msg);
                }
                sendChat("To specify a Coord or player: #worldId,x,y,z $PlayerName");
                sendChat("Best commands: cut d drop");
            }});
        add(new SubCommand ("go|gob|got") {
            @Override
            String details() {
                return "Teleports player to the center/bottom/top of the selection, in Hammerspace. Be ready to fly.";
            }
            @Override
            public void call(String[] args) {
                DSTeleporter tp = getTp();
                if (arg0.equalsIgnoreCase("gob")) {
                    tp.destination = selected.getCorner();
                } else if (arg0.equalsIgnoreCase("got")) {
                    tp.destination = selected.getFarCorner();
                } else {
                    tp.destination = selected.getCenter();
                }
                if (DimensionManager.getWorld(DeltaChunk.getDimensionId()) != player.worldObj) {
                    manager.transferPlayerToDimension(player, DeltaChunk.getDimensionId(), tp);
                } else {
                    tp.destination.x--;
                    tp.destination.moveToTopBlock();
                    player.setPositionAndUpdate(tp.destination.x + 0.5, tp.destination.y, tp.destination.z + 0.5);
                }
            }}, Requires.PLAYER, Requires.CREATIVE, Requires.SLICE_SELECTED);
        add(new SubCommand("enterhammer") {
            @Override
            String details() { return "Teleports the player into hammerspace"; }
            @Override
            void call(String[] args) {
                if (player.dimension == DeltaChunk.getDimensionId()) {
                    return;
                }
                DSTeleporter tp = getTp();
                tp.destination.set(DeltaChunk.getServerShadowWorld(), 0, 64, 0);
                manager.transferPlayerToDimension(player, DeltaChunk.getDimensionId(), tp);
            }}, Requires.PLAYER, Requires.CREATIVE);
        add(new SubCommand("leave", "[dest=0]") {
            @Override
            String details() { return "Teleports the player to the overworld"; }
            @Override
            void call(String[] args) {
                DSTeleporter tp = getTp();
                int targetDimId = 0;
                if (args.length == 1) {
                    targetDimId = Integer.parseInt(args[0]);
                }
                World w = DimensionManager.getWorld(targetDimId);
                ChunkCoordinates target = player.getBedLocation(targetDimId);
                if (target == null) {
                    target = w.getSpawnPoint(); 
                }
                if (target != null) {
                    tp.destination.set(target);
                }
                manager.transferPlayerToDimension(player, 0, tp);
            }}, Requires.PLAYER, Requires.CREATIVE);
        add(new SubCommand("jump") {
            @Override
            String details() { return "Warps player to the selection"; }
            @Override
            void call(String[] args) {
                DSTeleporter tp = getTp();
                tp.destination = new Coord(selected);
                manager.transferPlayerToDimension(player, selected.dimension, tp);
            }}, Requires.PLAYER, Requires.CREATIVE, Requires.SLICE_SELECTED);
        add(new SubCommand("tome") {
            @Override
            String details() { return "Warps selection to player"; }
            @Override
            void call(String[] args) {
                selected.posX = user.x;
                selected.posY = user.y;
                selected.posZ = user.z;
            }}, Requires.COORD, Requires.SLICE_SELECTED);
        add(new SubCommand("cut|copy", "x,y,z", "x,y,z") {
            @Override
            String details() { return "Creates a Slice from the range given"; }
            @Override
            void call(String[] args) {
                Coord base = new Coord(user.w, 0, 0, 0);
                final boolean copy = arg0.contains("copy");
                Coord low = base.add(DeltaCoord.parse(args[0]));
                Coord up = base.add(DeltaCoord.parse(args[1]));
                Coord.sort(low, up);
                DeltaCoord dimensions = up.difference(low);
                int area = Math.abs(dimensions.x*dimensions.y*dimensions.z);
                if (area > Hammer.max_fzds_grab_area) {
                    sendChat("The area is too big: " + area + "; max is " + Hammer.max_fzds_grab_area);
                    return;
                }
                final Coord lower = low.copy();
                final Coord upper = up.copy();
                if (player != null) {
                    new Notice(lower, "Low").send(player);
                    new Notice(upper, "High").send(player);
                }
                
                IDeltaChunk dse = DeltaChunk.makeSlice(Hammer.fzds_command_channel, lower, upper, new AreaMap() {
                    @Override
                    public void fillDse(DseDestination destination) {
                        Coord here = user.copy();
                        for (int x = lower.x; x <= upper.x; x++) {
                            for (int y = lower.y; y <= upper.y; y++) {
                                for (int z = lower.z; z <= upper.z; z++) {
                                    here.set(here.w, x, y, z);
                                    destination.include(here);
                                }
                            }
                        }
                    }}, !copy);
                dse.loadUsualCapabilities();
                dse.worldObj.spawnEntityInWorld(dse);
                setSelection(dse);
            }}, Requires.COORD);
        add(new SubCommand("movecenter", "x,y,z") {
            @Override
            void call(String[] args) {
                Vec3 newOffset = null;
                try {
                    String[] vecArg = args[0].split(",");
                    newOffset = new Vec3(
                            Double.parseDouble(vecArg[0]),
                            Double.parseDouble(vecArg[1]),
                            Double.parseDouble(vecArg[2]));
                } catch (Throwable e) {
                    Vec3 v = selected.getRotationalCenterOffset();
                    sendChat("Current rotational center: " + v.xCoord + "," + v.yCoord + "," + v.zCoord);
                }
                if (newOffset != null) {
                    selected.setRotationalCenterOffset(newOffset);
                }
            }
        }, Requires.SLICE_SELECTED);
        add(new SubCommand("grabchunk") {
            @Override
            String details() {
                return "Cuts out the chunk you're standing in";
            }
            @Override
            void call(String[] args) {
                Coord min = user.copy();
                min.x &= ~0xF;
                min.z &= ~0xF;
                min.y = 0;
                Coord max = min.copy();
                max.x += 0xF;
                max.z += 0xF;
                max.y = 0xFF;
                final Coord lower = min.copy();
                final Coord upper = max.copy();
                IDeltaChunk dse = DeltaChunk.makeSlice(Hammer.fzds_command_channel, lower, upper, new AreaMap() {
                    @Override
                    public void fillDse(DseDestination destination) {
                        Coord here = user.copy();
                        for (int x = lower.x; x <= upper.x; x++) {
                            for (int y = lower.y; y <= upper.y; y++) {
                                for (int z = lower.z; z <= upper.z; z++) {
                                    here.set(here.w, x, y, z);
                                    destination.include(here);
                                }
                            }
                        }
                    }}, true);
                dse.loadUsualCapabilities();
                dse.worldObj.spawnEntityInWorld(dse);
                setSelection(dse);
            }}, Requires.COORD);
        add(new SubCommand("include") { //TODO: would pull blocks into the slice
            @Override
            void call(String[] args) {
                //selected.get
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("drop", "[overwrite?]") {
            @Override
            String details() { return "Returns a Slice's blocks to the world, destroying the Slice"; }
            @Override
            void call(String[] args) {
                DeltaChunk.paste(selected, args.length >= 1);
                DeltaChunk.clear(selected);
                selected.setDead();
                setSelection(null);
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("paste", "[overwrite?]") {
            @Override
            String details() { return "Clones a Slice's blocks into the world"; }
            @Override
            void call(String[] args) {
                DeltaChunk.paste(selected, args.length > 1);
            }}, Requires.SLICE_SELECTED, Requires.CREATIVE);
        add(new SubCommand("oracle", "x,y,z", "x,y,z") {
            @Override
            void call(String[] args) {
                Coord base = new Coord(user.w, 0, 0, 0);
                Coord low = base.add(DeltaCoord.parse(args[0]));
                Coord up = base.add(DeltaCoord.parse(args[1]));
                AreaMap do_nothing = new AreaMap() {
                    @Override public void fillDse(DseDestination destination) { }
                };
                IDeltaChunk dse = DeltaChunk.makeSlice(Hammer.fzds_command_channel, low, up, do_nothing, false);
                dse.permit(DeltaCapability.ORACLE);
                dse.forbid(DeltaCapability.COLLIDE);
                user.setAsEntityLocation(dse);
                dse.worldObj.spawnEntityInWorld(dse);
            }}, Requires.OP);
        
        add(new SubCommand("grass") {
            @Override
            String details() { return "Places a grass block at the user's feet"; }
            @Override
            void call(String[] args) {
                user.add(0, -1, 0).setId(Blocks.grass);
            }}, Requires.COORD, Requires.CREATIVE);
        add(new SubCommand("snap") {
            @Override
            String details() { return "Rounds the Slice's position down to integers"; }
            @Override
            void call(String[] args) {
                selected.posX = (int) selected.posX;
                selected.posY = (int) selected.posY;
                selected.posZ = (int) selected.posZ;
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("removeall") {
            @Override
            String details() { return "Removes all Slices"; }
            @Override
            void call(String[] args) {
                int i = 0;
                for (World w : MinecraftServer.getServer().worldServers) {
                    for (Entity ent : (List<Entity>)w.loadedEntityList) {
                        if (ent instanceof DimensionSliceEntity) {
                            ent.setDead();
                            i++;
                            clearDseArea((DimensionSliceEntity) ent);
                        }
                    }
                }
                DeltaChunk.getSlices(MinecraftServer.getServer().worldServerForDimension(0)).clear();
                sendChat("Removed " + i);
            }}, Requires.OP);
        add(new SubCommand("selection") {
            @Override
            String details() { return "Prints the selection"; }
            @Override
            void call(String[] args) {
                sendChat("> " + selected);
                setSelection(selected);
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("rot?") {
            @Override
            String details() { return "Shows the rotation & angular velocity of the selection"; }
            @Override
            void call(String[] args) {
                sendChat("r = " + selected.getRotation());
                sendChat("ω = " + selected.getRotationalVelocity());
                if (!selected.can(DeltaCapability.ROTATE)) {
                    sendChat("(Does not have the ROTATE cap, so this is meaningless)");
                }
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("+|-") {
            @Override
            String details() { return "Changes which (loaded) Slice is selected"; }
            @Override
            void call(String[] args) {
                boolean add = arg0.equals("+");
                ArrayList<List<Entity>> entityLists = new ArrayList();
                for (World world : MinecraftServer.getServer().worldServers) {
                    if (world.isRemote) continue;
                    entityLists.add(world.loadedEntityList);
                }
                
                IDeltaChunk first = null, prev = null, next = null, last = null;
                boolean found_current = false;
                for (Entity ent : Iterables.concat(entityLists)) {
                    if (!(ent instanceof IDeltaChunk)) continue;
                    IDeltaChunk here = (IDeltaChunk) ent;
                    last = here;
                    if (first == null) {
                        first = last;
                    }
                    if (!found_current) {
                        prev = last;
                    }
                    if (found_current && next == null) {
                        next = last;
                    }
                    if (last == selected) {
                        found_current = true;
                    }
                }
                if (first == null) {
                    sendChat("There are no DSEs loaded");
                    setSelection(null);
                    return;
                }
                if (selected == null) {
                    //initialize selection
                    selected = add ? first : last;
                } else if (selected == last && add) {
                    selected = first;
                } else if (selected == first && !add) {
                    selected = last;
                } else {
                    selected = add ? next : prev;
                }
                sendChat("> " + selected);
                setSelection(selected);
                if (selected != null && player != null) {
                    new Notice(selected, "Selection").send(player);
                }
            }} /* needs nothing */);
        add(new SubCommand("select-nearest") {
            @Override
            void call(String[] args) {
                IDeltaChunk selected = null;
                double dist = Double.POSITIVE_INFINITY;
                for (IDeltaChunk idc : DeltaChunk.getAllSlices(user.w)) {
                    double d = user.distanceSq(new Coord(idc));
                    if (d > dist) continue;
                    dist = d;
                    selected = idc;
                }
                setSelection(selected);
            }
        }, Requires.COORD);
        add(new SubCommand("remove", "[part]") {
            @Override
            String details() { return "Destroys the selection and everything attatched, unless 'part' is given"; }
            @Override
            void call(String[] args) {
                boolean recursive = true;
                if (args.length == 1 && args[0].equalsIgnoreCase("part")) {
                    recursive = false;
                }
                HashSet<IDeltaChunk> toKill = new HashSet<IDeltaChunk>();
                toKill.add(selected);
                if (recursive) {
                    boolean any = true;
                    while (any) {
                        any = false;
                        ArrayList<IDeltaChunk> toAdd = new ArrayList<IDeltaChunk>();
                        for (IDeltaChunk idc : toKill) {
                            IDeltaChunk parent = idc.getParent();
                            if (parent != null) toAdd.add(parent);
                            List<IDeltaChunk> children = idc.getChildren();
                            if (children == null) continue;
                            toAdd.addAll(children);
                        }
                        int firstSize = toKill.size();
                        toKill.addAll(toAdd);
                        any = firstSize != toKill.size();
                    }
                }

                for (IDeltaChunk idc : toKill) {
                    idc.setDead();
                    clearDseArea(idc);
                }
                setSelection(null);
                sendChat("Made dead");
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("sr|sw", "angle°", "[direction=UP]") {
            @Override
            String details() { return "Sets the Slice's rotation"; }
            @Override
            void call(String[] args) {
                if (args.length != 2 && args.length != 1) {
                    throw new SyntaxErrorException();
                }
                if (!selected.can(DeltaCapability.ROTATE)) {
                    sendChat("Selection does not have the rotation cap");
                    return;
                }
                double theta = Math.toRadians(Double.parseDouble(args[0]));
                EnumFacing dir;
                try {
                    if (args.length == 2) {
                        dir = EnumFacing.valueOf(args[1].toUpperCase(Locale.ROOT));
                    } else {
                        dir = EnumFacing.UP;
                    }
                } catch (IllegalArgumentException e) {
                    String msg = "Direction must be:";
                    for (EnumFacing d : EnumFacing.values()) {
                        if (d == null) {
                            continue;
                        }
                        msg += " " + d;
                    }
                    sendChat(msg);
                    return;
                }
                Quaternion newVal = Quaternion.getRotationQuaternionRadians(theta, dir);
                if (arg0.equalsIgnoreCase("sr")) {
                    selected.setRotation(newVal);
                } else if (arg0.equalsIgnoreCase("sw")) {
                    selected.setRotationalVelocity(newVal);
                } else {
                    throw new SyntaxErrorException();
                }
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("d|s|v|r|w", "+|=", "[W=1]", "X", "Y", "Z") {
            @Override
            String details() { return "Changes or sets displacement/velocity/rotation/angular_velocity"; }
            @Override
            void call(String[] args) {
                char type = arg0.charAt(0);
                type = type == 's' ? 'd' : type;
                if (args.length != 4 && args.length != 5) {
                    if (type == 'd' || type == 'v') {
                        sendChat("Usage: /fzds d(isplacement)|v(elocity) +|= X Y Z");
                    }
                    if (type == 'r' || type == 'w') {
                        sendChat("Usage: /fzds r(otation)|w(rotational velocity) +|= [W=1] X Y Z (a quaternion; cmds sr & sw are simpler)");
                    }
                    return;
                }
                int i = 0;
                double w = 1;
                if (args.length == 5) {
                    w = Double.parseDouble(args[1]);
                    i = 1;
                }
                double x = Double.parseDouble(args[1+i]);
                double y = Double.parseDouble(args[2+i]);
                double z = Double.parseDouble(args[3+i]);
                if ((type == 'r' || type == 'w') && !selected.can(DeltaCapability.ROTATE)) {
                    sendChat("Selection does not have the ROTATE cap");
                    return;
                }
                if (args[0].equals("+")) {
                    if (type == 'd' || type == 's') {
                        selected.setPosition(selected.posX + x, selected.posY + y, selected.posZ + z);
                    } else if (type == 'v') {
                        selected.addVelocity(x/20, y/20, z/20);
                    } else if (type == 'r') {
                        selected.getRotation().incrAdd(new Quaternion(w, x, y, z));
                    } else if (type == 'w') {
                        selected.getRotationalVelocity().incrAdd(new Quaternion(w, x, y, z));
                    } else {
                        sendChat("Not a command?");
                    }
                } else if (args[0].equals("=")) {
                    if (type == 'd' || type == 's') {
                        selected.setPosition(x, y, z);
                    } else if (type == 'v') {
                        selected.motionX = 0;
                        selected.motionY = 0;
                        selected.motionZ = 0;
                        selected.addVelocity(x/20, y/20, z/20);
                    } else if (type == 'r') {
                        selected.setRotation((new Quaternion(w, x, y, z)));
                    } else if (type == 'w') {
                        Quaternion omega = (new Quaternion(w, x, y, z));
                        selected.setRotationalVelocity(omega);
                    } else {
                        sendChat("Not a command?");
                    }
                    selected.getRotation().incrNormalize();
                    selected.getRotationalVelocity().incrNormalize();
                } else {
                    sendChat("+ or =?");
                }
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("dirty") {
            @Override
            String details() { return "[Moves the selection back and forth]"; }
            @Override
            void call(String[] args) {
                selected.getRotationalVelocity().w *= -1;
                selected.getRotation().w *= -1;
                selected.getRotation().w += 0.1;
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("caps") {
            @Override
            String details() { return "Lists the available capabilities"; }
            @Override
            void call(String[] args) {
                String r = "";
                for (DeltaCapability cap : DeltaCapability.values()) {
                    r += " " + cap;
                }
                sendChat(r);
            }});
        add(new SubCommand("cap?") {
            @Override
            String details() { return "Lists the capabilities enabled on the selection"; }
            @Override
            void call(String[] args) {
                String r = "";
                for (DeltaCapability cap : DeltaCapability.values()) {
                    if (selected.can(cap)) {
                        r += " " + cap;
                    }
                }
                sendChat(r);
            }}, Requires.SLICE_SELECTED);
        add(new SubCommand("cap+|cap-", "CAP+") {
            @Override
            String details() { return "Gives or takes away capabilities. (May cause client desyncing.)"; }
            @Override
            void call(String[] args) {
                for (String a : args) {
                    DeltaCapability cap = DeltaCapability.valueOf(a);
                    if (arg0.equalsIgnoreCase("cap+")) {
                        selected.permit(cap);
                    } else {
                        selected.forbid(cap);
                    }
                }
            }}, Requires.SLICE_SELECTED, Requires.OP);
        add(new SubCommand("incrScale", "newScale") {
            @Override
            void call(String[] args) {
                if (!selected.can(DeltaCapability.SCALE)) {
                    sendChat("Selection doesn't have the SCALE cap");
                    return;
                }
                ((DimensionSliceEntity) selected).scale = Float.parseFloat(args[0]);
            }}, Requires.SLICE_SELECTED, Requires.CREATIVE);
        add(new SubCommand("alpha", "newOpacity") {
            @Override
            void call(String[] args) {
                if (!selected.can(DeltaCapability.TRANSPARENT)) {
                    sendChat("Selection doesn't have the TRANSPARENT cap");
                    return;
                }
                ((DimensionSliceEntity) selected).opacity = Float.parseFloat(args[0]);
            }}, Requires.SLICE_SELECTED, Requires.CREATIVE);
        add(new SubCommand("setBlockMethod", "mode") {
            @Override
            String details() {
                return "0=lowlevel 1=world.isRemote 2=world.setBlock 3=world.setBlock2+flags";
            }
            @Override
            void call(String[] args) {
                int mode = Integer.parseInt(args[0]);
                sendChat("setBlockMethod was " + TransferLib.default_set_method + ", is now " + mode);
                TransferLib.default_set_method = mode;
            }}, Requires.OP, Requires.CREATIVE);
        add(new SubCommand("@", "name [position|'unset']") {
            @Override
            String details() {
                return "Set position variables ('@name' gets replaced with position)";
            }
            @Override
            void call(String[] args) {
                String name = args[0];
                Coord val = user;
                if (args.length == 2) {
                    String parse = args[1];
                    if (parse.equalsIgnoreCase("unset")) {
                        if (positionVariables.remove(name) != null) {
                            sendChat("Unset: " + name);
                        } else {
                            sendChat("Didn't exist: " + name);
                        }
                        return;
                    }
                    val = parseCoord(user.w, parse);
                }
                positionVariables.put(name, val);
                sendChat("@" + name + " = " + val);
                if (player != null) {
                    new Notice(val, name).send(player);
                }
            }}, Requires.COORD);
        add(new SubCommand("@?|@??", "[search]") {
            @Override
            String details() {
                return "Show, and maybe list, position variables";
            }
            @Override
            void call(String[] args) {
                boolean print = arg0.equalsIgnoreCase("@??");
                String search = "";
                if (args.length == 1) {
                    search = args[0];
                }
                for (Entry<String, Coord> var : positionVariables.entrySet()) {
                    String name = var.getKey();
                    Coord pos = var.getValue();
                    if (pos.w != user.w) {
                        continue;
                    }
                    if (!name.contains(search)) {
                        continue;
                    }
                    if (player != null) {
                        new Notice(pos, name).send(player);
                    }
                    if (print) {
                        sendChat(name + ": " + pos);
                    }
                }
            }}, Requires.COORD, Requires.PLAYER);
        add(new SubCommand("construct", "x,y,z", "x,y,z") {
            @Override
            String details() {
                return "Create a DSE from Hammerspace coordinates";
            }

            @Override
            void call(String[] args) {
                Coord origin = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
                Coord low = origin.add(DeltaCoord.parse(args[0]));
                Coord upr = origin.add(DeltaCoord.parse(args[1]));
                Coord.sort(low, upr);
                IDeltaChunk dse = DeltaChunk.construct(user.w, low, upr);
                dse.loadUsualCapabilities();
                user.setAsEntityLocation(dse);
                user.w.spawnEntityInWorld(dse);
            }
        }, Requires.COORD);
        add(new SubCommand("orbitme") {
            @Override
            void call(String[] args) {
                Vec3 p = new Vec3(player.posX, player.posY, player.posZ);
                selected.changeRotationCenter(p);
            }
        }, Requires.SLICE_SELECTED, Requires.PLAYER);
        add(new SubCommand("resetbody") {
            @Override
            String details() {
                return "Resets the rotation of a DSE & its children";
            }
            
            @Override
            void call(String[] args) {
                selected.cancelOrderedRotation();
                selected.setRotation(new Quaternion());
                selected.setRotationalVelocity(new Quaternion());
                for (IDeltaChunk idc : selected.getChildren()) {
                    Vec3 base = idc.getParentJoint();
                    Vec3 snapTo = idc.shadow2real(base);
                    SpaceUtil.toEntPos(idc, snapTo);
                    selected = idc;
                    this.call(args);
                }
            }
            
        }, Requires.SLICE_SELECTED);
        add(new SubCommand("setParent") {
            @Override
            String details() {
                return "Sets the selected DSE's parent by the parent's ID";
            }

            @Override
            void call(String[] args) {
                int id = Integer.parseInt(args[0]);
                Entity ent = selected.worldObj.getEntityByID(id);
                if (!(ent instanceof IDeltaChunk)) {
                    sendChat("ID did not point to a DSE");
                    return;
                }
                IDeltaChunk parent = (IDeltaChunk) ent;
                if (parent == selected) {
                    sendChat("Can't parent to itself");
                    return;
                }
                selected.setParent(parent);
            }
            
        }, Requires.SLICE_SELECTED);
        if (Core.dev_environ) add(new SubCommand("test") {
            @Override
            void call(String[] args) {
                if (args.length > 0 && args[0].equals("cam")) {
                    Minecraft.getMinecraft().gameSettings.debugCamEnable ^= true;
                    return;
                }
                double angle = Math.PI * selected.worldObj.rand.nextDouble();
                EnumFacing up = EnumFacing.UP;
                int time = 30;
                if (selected.getParent() == null) {
                    up = EnumFacing.NORTH;
                    time = 300;
                }
                Quaternion quat = Quaternion.getRotationQuaternionRadians(angle, up);
                selected.orderTargetRotation(quat, time, Interpolation.SMOOTH);
            }
            
        }, Requires.SLICE_SELECTED);
        add(new SubCommand("setbiome", "x,y,z", "x,y,z", "biomeId") {
            @Override
            String details() {
                return "Changes the biome ID; see the manual's reference section for biomes";
            }

            @Override
            void call(String[] args) {
                Coord origin = new Coord(world, 0, 0, 0);
                Coord low = origin.add(DeltaCoord.parse(args[0]));
                Coord upr = origin.add(DeltaCoord.parse(args[1]));
                Coord.sort(low, upr);
                low.y = upr.y;
                int biomeId = Integer.parseInt(args[2]);
                final BiomeGenBase biome = BiomeGenBase.getBiome(biomeId);
                Coord.iterateCube(low, upr, new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        here.setBiome(biome);
                    }
                });

                Coord.iterateChunks(low, upr, new ICoordFunction() {
                    @Override
                    public void handle(Coord here) {
                        here.resyncChunksFull();
                    }
                });
            }
        }, Requires.CREATIVE, Requires.PLAYER);
    }
    
    private static void clearDseArea(IDeltaChunk idc) {
        Coord a = idc.getCorner();
        Coord b = idc.getFarCorner();
        if (a.w == null || b.w == null) {
            Core.logSevere("DSE's area doesn't have the worlds set? Can't wipe its area. " + idc);
            return;
        }
        Coord.iterateCube(a, b, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                here.setAir();
            }
        });
    }
}
