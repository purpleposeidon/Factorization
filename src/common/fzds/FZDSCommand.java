package factorization.fzds;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.Quaternion;
import factorization.common.Core;
import factorization.common.FactorizationUtil;
import factorization.fzds.DeltaChunk.AreaMap;
import factorization.fzds.DeltaChunk.DseDestination;
import factorization.fzds.api.DeltaCapability;
import factorization.fzds.api.IDeltaChunk;

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
        ArrayList<String> altNames = new ArrayList();
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
        
        static ServerConfigurationManager manager = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer());
        
        String arg0;
        ICommandSender sender;
        EntityPlayerMP player;
        World world;
        Coord user;
        boolean op;
        boolean creative;
        IDeltaChunk selected;
        
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
            sender.sendChatToPlayer(msg);
        }
    }
    
    public static enum Requires {
        OP, PLAYER, COORD, SELECTION, CREATIVE;
        
        void apply(SubCommand sc) {
            switch (this) {
            case OP: sc.needOp = true; break;
            case PLAYER: sc.needPlayer = true; break;
            case COORD: sc.needCoord = true; break;
            case SELECTION: sc.needSelection = true; break;
            case CREATIVE: sc.needCreative = true; break;
            }
        }
    }
    
    private static ArrayList<SubCommand> subCommands = new ArrayList();
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
    public void processCommand(ICommandSender sender, String[] args) {
        if (sender instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) sender;
            boolean op = MinecraftServer.getServer().getConfigurationManager().areCommandsAllowed(player.username);
            boolean cr = player.capabilities.isCreativeMode;
            if (!(op || cr)) {
                sender.sendChatToPlayer("You must be op or in creative mode to use these commands");
                return;
            }
        }
        if (args.length == 0) {
            runCommand(help, sender, new String[] {"help"});
            return;
        }
        String cmd = args[0];
        for (SubCommand sc : subCommands) {
            for (String alias : sc.altNames) {
                if (alias.equalsIgnoreCase(cmd)) {
                    runCommand(sc, sender, args);
                    return;
                }
            }
        }
        sender.sendChatToPlayer("Not a command");
    }
    
    private static WeakReference<IDeltaChunk> currentSelection = new WeakReference(null);
    
    public static void setSelection(IDeltaChunk dse) {
        currentSelection = new WeakReference(dse);
    }
    
    private static Splitter comma = Splitter.on(",");
    void runCommand(SubCommand cmd, ICommandSender sender, String[] args) {
        cmd.reset();
        cmd.setup(sender);
        ArrayList<String> cleanedArgs = new ArrayList();
        for (String a : args) {
            if (Strings.isNullOrEmpty(a)) {
                continue;
            } else if (a.startsWith("@")) {
                //set the player
                if (!cmd.op) {
                    throw new CommandException("You are not allowed to use arbitrary players");
                }
                cmd.player = MinecraftServer.getServer().getConfigurationManager().getPlayerForUsername(a.substring(1));
                if (cmd.player == null) {
                    throw new CommandException("Player not found");
                } 
            } else if (a.startsWith("#")) {
                if (!cmd.op) {
                    throw new CommandException("You are not allowed to use arbitrary locations");
                }
                ArrayList<Integer> parts = new ArrayList();
                for (String stupid : comma.split(a.substring(1))) {
                    parts.add(Integer.parseInt(stupid));
                }
                World w = DimensionManager.getWorld(parts.get(0));
                cmd.user = new Coord(w, parts.get(1), parts.get(2), parts.get(3));
            } else {
                cleanedArgs.add(a);
            }
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
        if (cmd.needOp && cmd.op == false) {
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
        String ret = "";
        for (SubCommand sc : cmd) {
            ret += " ";
            if (sc.help.length == 1) {
                ret += sc.help[0];
            } else {
                ret += "(" + sc.help[0];
                for (int i = 1; i < sc.help.length; i++) {
                    ret += " " + sc.help[i];
                }
                ret += ")";
            }
        }
        return ret;
    }
    
    private static String pick(String ...bits) {
        for (int i = 0; i < bits.length - 1; i += 2) {
            if (bits[i].equalsIgnoreCase(bits[i + 1])) {
                return bits[i + 1];
            }
        }
        return bits[bits.length - 1];
    }
    
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
                sender.sendChatToPlayer(join(good));
                sender.sendChatToPlayer("To specify a Coord or player: #worldId,x,y,z @PlayerName");
                sender.sendChatToPlayer("Best commands: cut d drop");
            }});
        add(new SubCommand ("go|gob|got") {
            @Override
            String details() {
                return "Teleports player to the " + pick("gob", "bottom", "got", "top", "center") + " of the selection, in Hammerspace. Be ready to fly.";
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
                if (DimensionManager.getWorld(Core.dimension_slice_dimid) != player.worldObj) {
                    manager.transferPlayerToDimension(player, Core.dimension_slice_dimid, tp);
                } else {
                    tp.destination.x--;
                    tp.destination.moveToTopBlock();
                    player.setPositionAndUpdate(tp.destination.x + 0.5, tp.destination.y, tp.destination.z + 0.5);
                }
            }}, Requires.PLAYER, Requires.CREATIVE, Requires.SELECTION);
        add(new SubCommand("enterhammer") {
            @Override
            String details() { return "Teleports the player into hammerspace"; }
            @Override
            void call(String[] args) {
                if (player.dimension == Hammer.dimensionID) {
                    return;
                }
                DSTeleporter tp = getTp();
                tp.destination.set(DeltaChunk.getServerShadowWorld(), 0, 64, 0);
                manager.transferPlayerToDimension(player, Hammer.dimensionID, tp);
            }}, Requires.PLAYER, Requires.CREATIVE);
        add(new SubCommand("leave") {
            @Override
            String details() { return "Teleports the player to the overworld"; }
            @Override
            void call(String[] args) {
                DSTeleporter tp = getTp();
                World w = DimensionManager.getWorld(0);
                if (w == player.worldObj) {
                    return;
                }
                ChunkCoordinates target = player.getBedLocation();
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
            }}, Requires.PLAYER, Requires.CREATIVE, Requires.SELECTION);
        add(new SubCommand("tome") {
            @Override
            String details() { return "Warps selection to player"; }
            @Override
            void call(String[] args) {
                selected.posX = user.x;
                selected.posY = user.y;
                selected.posZ = user.z;
            }}, Requires.COORD, Requires.SELECTION);
        add(new SubCommand("cut|rcut|copy|rcopy", "x,y,z", "x,y,z") {
            @Override
            String details() { return "Creates a Slice from the range given" + pick("rcut", ", relative to user's position", ""); }
            @Override
            void call(String[] args) {
                Coord base;
                if (arg0.startsWith("r")) {
                    base = user.copy();
                } else {
                    base = new Coord(user.w, 0, 0, 0);
                }
                boolean copy = arg0.contains("copy");
                Coord low = base.add(DeltaCoord.parse(args[0]));
                Coord up = base.add(DeltaCoord.parse(args[1]));
                Coord.sort(low, up);
                DeltaCoord dimensions = up.difference(low);
                int area = Math.abs(dimensions.x*dimensions.y*dimensions.z);
                if (area > Hammer.max_fzds_grab_area) {
                    sender.sendChatToPlayer("The area is too big: " + area);
                    return;
                }
                final Coord lower = low.copy();
                final Coord upper = up.copy();
                Core.notify(null, lower, "Low");
                Core.notify(null, upper, "High");
                
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
                dse.permit(DeltaCapability.ROTATE).forbid(DeltaCapability.COLLIDE);
                dse.worldObj.spawnEntityInWorld(dse);
                setSelection(dse);
            }}, Requires.COORD);
        add(new SubCommand("drop") {
            @Override
            String details() { return "Returns a Slice's blocks to the world, destroying the Slice"; }
            @Override
            void call(String[] args) {
                DeltaChunk.paste(selected, true);
                DeltaChunk.clear(selected);
                selected.setDead();
                setSelection(null);
            }}, Requires.SELECTION);
        add(new SubCommand("paste") {
            @Override
            String details() { return "Clones a Slice's blocks into the world"; }
            @Override
            void call(String[] args) {
                DeltaChunk.paste(selected, true);
            }}, Requires.SELECTION, Requires.CREATIVE);
        add(new SubCommand("oracle") {
            @Override
            void call(String[] args) {
                AreaMap do_nothing = new AreaMap() {
                    @Override public void fillDse(DseDestination destination) { }
                };
                IDeltaChunk dse = DeltaChunk.makeSlice(Hammer.fzds_command_channel, user, user, do_nothing, false);
                dse.permit(DeltaCapability.ORACLE);
                dse.worldObj.spawnEntityInWorld(dse);
            }}, Requires.OP);
        
        add(new SubCommand("grass") {
            @Override
            String details() { return "Places a grass block at the user's feet"; }
            @Override
            void call(String[] args) {
                user.add(0, -1, 0).setId(Block.grass);
            }}, Requires.COORD, Requires.CREATIVE);
        add(new SubCommand("snap") {
            @Override
            String details() { return "Rounds the Slice's position down to integers"; }
            @Override
            void call(String[] args) {
                selected.posX = (int) selected.posX;
                selected.posY = (int) selected.posY;
                selected.posZ = (int) selected.posZ;
            }}, Requires.SELECTION);
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
                        }
                    }
                }
                sender.sendChatToPlayer("Removed " + i);
            }}, Requires.OP);
        add(new SubCommand("selection") {
            @Override
            String details() { return "Prints the selection"; }
            @Override
            void call(String[] args) {
                sender.sendChatToPlayer("> " + selected);
                setSelection(selected);
            }}, Requires.SELECTION);
        add(new SubCommand("rot?") {
            @Override
            String details() { return "Gives the rotation & angular velocity of the selection"; }
            @Override
            void call(String[] args) {
                sender.sendChatToPlayer("r = " + selected.getRotation());
                sender.sendChatToPlayer("ω = " + selected.getRotationalVelocity());
                if (!selected.can(DeltaCapability.ROTATE)) {
                    sender.sendChatToPlayer("(Does not have the ROTATE cap, so this is meaningless)");
                }
            }}, Requires.SELECTION);
        add(new SubCommand("+|-") {
            @Override
            String details() { return "Changes which (loaded) Slice is selected"; }
            @Override
            void call(String[] args) {
                boolean add = arg0.equals("+");
                Iterator<IDeltaChunk> it = DeltaChunk.getSlices(MinecraftServer.getServer().worldServerForDimension(0)).iterator();
                IDeltaChunk first = null, prev = null, next = null, last = null;
                boolean found_current = false;
                while (it.hasNext()) {
                    IDeltaChunk here = it.next();
                    if (here.isDead) {
                        Core.logWarning(here + " was not removed");
                        it.remove();
                        continue;
                    }
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
                    sender.sendChatToPlayer("There are no DSEs loaded");
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
                sender.sendChatToPlayer("> " + selected);
                setSelection(selected);
            }} /* needs nothing */);
        add(new SubCommand("remove") {
            @Override
            String details() { return "Destroys the selection"; }
            @Override
            void call(String[] args) {
                selected.setDead();
                setSelection(null);
                sender.sendChatToPlayer("Made dead");
            }}, Requires.SELECTION);
        add(new SubCommand("sr|sw", "angle°", "[direction=UP]") {
            @Override
            String details() { return "Sets the Slice's rotation"; }
            @Override
            void call(String[] args) {
                if (args.length != 2 && args.length != 1) {
                    throw new SyntaxErrorException();
                }
                if (!selected.can(DeltaCapability.ROTATE)) {
                    sender.sendChatToPlayer("Selection does not have the rotation cap");
                    return;
                }
                double theta = Math.toRadians(Double.parseDouble(args[0]));
                ForgeDirection dir;
                try {
                    if (args.length == 2) {
                        dir = ForgeDirection.valueOf(args[1].toUpperCase());
                    } else {
                        dir = ForgeDirection.UP;
                    }
                } catch (IllegalArgumentException e) {
                    String msg = "Direction must be:";
                    for (ForgeDirection d : ForgeDirection.values()) {
                        if (d == ForgeDirection.UNKNOWN) {
                            continue;
                        }
                        msg += " " + d;
                    }
                    sender.sendChatToPlayer(msg);
                    return;
                }
                int derivative = -1;
                if (arg0.equalsIgnoreCase("sr")) {
                    derivative = 0;
                } else if (arg0.equalsIgnoreCase("sw")) {
                    derivative = 1;
                } else {
                    throw new SyntaxErrorException();
                }
                Quaternion toMod = derivative == 0 ? selected.getRotation() : selected.getRotationalVelocity();
                toMod.update(Quaternion.getRotationQuaternion(theta, dir));
            }}, Requires.SELECTION);
        add(new SubCommand("d|v|r|w", "+|=", "[W=1]", "X", "Y", "Z") {
            @Override
            String details() { return "Changes or sets displacement/velocity/rotation/angular_velocity"; }
            @Override
            void call(String[] args) {
                char type = arg0.charAt(0);
                if (args.length != 4 && args.length != 5) {
                    if (type == 'd' || type == 'v') {
                        sender.sendChatToPlayer("Usage: /fzds d(isplacement)|v(elocity) +|= X Y Z");
                    }
                    if (type == 'r' || type == 'w') {
                        sender.sendChatToPlayer("Usage: /fzds r(otation)|w(rotational velocity) +|= [W=1] X Y Z (a quaternion; cmds sr & sw are simpler)");
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
                    sender.sendChatToPlayer("Selection does not have the ROTATE cap");
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
                        sender.sendChatToPlayer("Not a command?");
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
                        sender.sendChatToPlayer("Not a command?");
                    }
                    selected.getRotation().incrNormalize();
                    selected.getRotationalVelocity().incrNormalize();
                } else {
                    sender.sendChatToPlayer("+ or =?");
                }
            }}, Requires.SELECTION);
        add(new SubCommand("dirty") {
            @Override
            String details() { return "[Moves the selection back and forth]"; }
            @Override
            void call(String[] args) {
                selected.getRotationalVelocity().w *= -1;
                selected.getRotation().w *= -1;
                selected.getRotation().w += 0.1;
            }}, Requires.SELECTION);
        add(new SubCommand("caps") {
            @Override
            String details() { return "Lists the available Caps"; }
            @Override
            void call(String[] args) {
                String r = "";
                for (DeltaCapability cap : DeltaCapability.values()) {
                    r += " " + cap;
                }
                sender.sendChatToPlayer(r);
            }});
        add(new SubCommand("cap?") {
            @Override
            String details() { return "Lists the Caps enabled on the selection"; }
            @Override
            void call(String[] args) {
                String r = "";
                for (DeltaCapability cap : DeltaCapability.values()) {
                    if (selected.can(cap)) {
                        r += " " + cap;
                    }
                }
                sender.sendChatToPlayer(r);
            }}, Requires.SELECTION);
        add(new SubCommand("cap+|cap-", "CAP+") {
            @Override
            String details() { return "Gives or takes away Caps. May cause client desyncing."; }
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
            }}, Requires.SELECTION, Requires.OP);
        add(new SubCommand("scale", "newscale") {
            @Override
            void call(String[] args) {
                if (!selected.can(DeltaCapability.SCALE)) {
                    sender.sendChatToPlayer("Selection doesn't have the SCALE cap");
                    return;
                }
                ((DimensionSliceEntity) selected).scale = Float.parseFloat(args[0]);
            }}, Requires.SELECTION, Requires.CREATIVE);
        add(new SubCommand("alpha", "newOpacity") {
            @Override
            void call(String[] args) {
                if (!selected.can(DeltaCapability.TRANSPARENT)) {
                    sender.sendChatToPlayer("Selection doesn't have the TRANSPARENT cap");
                    return;
                }
                ((DimensionSliceEntity) selected).opacity = Float.parseFloat(args[0]);
            }}, Requires.SELECTION, Requires.CREATIVE);
    }

}
