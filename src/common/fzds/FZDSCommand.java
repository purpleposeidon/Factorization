package factorization.fzds;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
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
import factorization.fzds.DimensionSliceEntity.Caps;

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
        DimensionSliceEntity selected;
        
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
        
        DSTeleporter getTp() {
            DSTeleporter tp = new DSTeleporter((WorldServer) player.worldObj);
            tp.destination = new Coord(player.worldObj, 0, 0, 0);
            return tp;
        }
        
        boolean appropriate() {
            return true;
        }
        
        String details() {
            return null;
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
    
    private static WeakReference<DimensionSliceEntity> currentSelection = new WeakReference(null);
    
    public static void setSelection(DimensionSliceEntity dse) {
        currentSelection = new WeakReference(dse);
    }
    
    private static Splitter comma = Splitter.on(",");
    void runCommand(SubCommand cmd, ICommandSender sender, String[] args) {
        cmd.reset();
        cmd.sender = sender;
        if (sender instanceof EntityPlayerMP) {
            cmd.player = (EntityPlayerMP) sender;
        }
        if (sender instanceof TileEntity) {
            cmd.user = new Coord((TileEntity) sender);
        }
        cmd.selected = currentSelection.get();
        if (sender.canCommandSenderUseCommand(4, "stop")) {
            cmd.op = true;
        }
        if (sender instanceof TileEntityCommandBlock) {
            cmd.op = true;
        }
        if (cmd.op) {
            cmd.creative = true;
        }
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
            } else if (a.startsWith("%")) {
                World w = null;
                int id = Integer.parseInt(a.substring(1));
                boolean found = false;
                for (DimensionSliceEntity dse : Hammer.getSlices(null /* not specifying world is LAME here */)) {
                    if (dse.cell == id) {
                        cmd.selected = dse;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new CommandException("Did not find that DSE");
                }
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
        if (cmd.needPlayer) {
            if (cmd.player == null) {
                throw new CommandException("No player specified");
            }
        }
        if (cmd.needCoord) {
            if (cmd.user == null) {
                throw new CommandException("No coordinate specified");
            }
        }
        if (cmd.needOp) {
            if (cmd.op == false) {
                throw new CommandException("Insufficient permissions");
            }
        }
        if (cmd.needSelection) {
            if (cmd.selected == null) {
                throw new CommandException("No DSE selected");
            }
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
    
    static {
        help = add(new SubCommand("help") {
            @Override
            void call(String[] args) {
                sender.sendChatToPlayer(join(subCommands));
                sender.sendChatToPlayer("The commands need a Coord, DSE, or player.");
                sender.sendChatToPlayer("If these are not implicitly available, you can provide them using:");
                sender.sendChatToPlayer(" #worldId,x,y,z %CellId @PlayerName");
                sender.sendChatToPlayer("Best commands: grab goc leave drop");
            }});
        add(new SubCommand ("go|goc", "[id=0]") {
            @Override
            public void call(String[] args) {
                int destinationCell = 0;
                if (args.length > 0) {
                    destinationCell = Integer.parseInt(args[0]);
                } else if (selected != null) {
                    destinationCell = selected.cell;
                }
                DSTeleporter tp = getTp();
                if (arg0.equalsIgnoreCase("goc")) {
                    tp.destination = Hammer.getCellCenter(player.worldObj, destinationCell);
                } else {
                    tp.destination = Hammer.getCellLookout(player.worldObj, destinationCell);
                }
                if (DimensionManager.getWorld(Core.dimension_slice_dimid) != player.worldObj) {
                    manager.transferPlayerToDimension(player, Core.dimension_slice_dimid, tp);
                } else {
                    tp.destination.x--;
                    tp.destination.moveToTopBlock();
                    player.setPositionAndUpdate(tp.destination.x + 0.5, tp.destination.y, tp.destination.z + 0.5);
                }
            }}, Requires.PLAYER, Requires.CREATIVE);
        add(new SubCommand("leave") {
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
                Vec3 v = Vec3.createVectorHelper(target.posX, target.posY + 1, target.posZ);
                //HammerNet.transferPlayer(player, (DimensionSliceEntity)null, w, v);
                if (target != null) {
                    tp.destination.set(target);
                }
                manager.transferPlayerToDimension(player, 0, tp);
            }}, Requires.PLAYER, Requires.CREATIVE);
        add(new SubCommand("tome") {
            @Override
            void call(String[] args) {
                selected.posX = user.x;
                selected.posY = user.y;
                selected.posZ = user.z;
            }}, Requires.COORD, Requires.SELECTION);
        add(new SubCommand("grab|rgrab", "x,y,z", "x,y,z") {
            @Override
            void call(String[] args) {
                Coord base;
                if (arg0.equalsIgnoreCase("rgrab")) {
                    base = user.copy();
                } else {
                    base = new Coord(user.w, 0, 0, 0);
                }
                Coord a = base.add(DeltaCoord.parse(args[0]));
                Coord b = base.add(DeltaCoord.parse(args[1]));
                Coord lower, upper;
                if (a.isSubmissiveTo(b)) {
                    lower = a;
                    upper = b;
                } else {
                    upper = b;
                    lower = a;
                }
                DimensionSliceEntity dse = Hammer.allocateSlice(user.w).permit(Caps.ROTATE);
                Coord middle = lower.add(upper.difference(lower).scale(0.5));
                middle.setAsEntityLocation(dse);
                dse.posX += 0.5;
                dse.posZ += 0.5;
                
                
                Coord corner = Hammer.getCellCorner(user.w, dse.cell);
                Coord far = Hammer.getCellOppositeCorner(user.w, dse.cell);
                Coord c = new Coord(Hammer.getServerShadowWorld(), 0, 0, 0);
                Coord r = new Coord(user.w, 0, 0, 0);
                for (int x = lower.x; x <= upper.x; x++) {
                    for (int y = lower.y; y <= upper.y; y++) {
                        for (int z = lower.z; z <= upper.z; z++) {
                            Vec3 real = Vec3.createVectorHelper(x, y, z);
                            r.set(real);
                            Vec3 shadow = dse.real2shadow(real);
                            c.set(shadow);
                            TransferLib.move(r, c);
                        }
                    }
                }
                for (int x = lower.x; x <= upper.x; x++) {
                    for (int y = lower.y; y <= upper.y; y++) {
                        for (int z = lower.z; z <= upper.z; z++) {
                            Vec3 real = Vec3.createVectorHelper(x, y, z);
                            r.set(real);
                            Vec3 shadow = dse.real2shadow(real);
                            c.set(shadow);
                            c.markBlockForUpdate();
                        }
                    }
                }
                dse.worldObj.spawnEntityInWorld(dse);
                setSelection(dse);
            }}, Requires.COORD);
        add(new SubCommand("drop") {			
            @Override
            void call(String[] args) {
                Coord a = new Coord(Hammer.getServerShadowWorld(), 0, 0, 0);
                Coord b = a.copy();
                Vec3 vShadowMin = FactorizationUtil.getMin(selected.shadowArea);
                Vec3 vShadowMax = FactorizationUtil.getMax(selected.shadowArea);
                a.set(vShadowMin);
                b.set(vShadowMax);
                Vec3 shadow = Vec3.createVectorHelper(0, 0, 0);
                DeltaCoord dc = b.difference(a);
                Coord dest = new Coord(selected);
                
                for (int x = a.x; x < b.x; x++) {
                    for (int y = a.y; y < b.y; y++) {
                        for (int z = a.z; z < b.z; z++) {
                            Coord c = new Coord(a.w, x, y, z);
                            c.setAsVector(shadow);
                            Vec3 real = selected.shadow2real(shadow);
                            dest.set(real);
                            TransferLib.move(c, dest);
                        }
                    }
                }
                for (int x = a.x; x < b.x; x++) {
                    for (int y = a.y; y < b.y; y++) {
                        for (int z = a.z; z < b.z; z++) {
                            Coord c = new Coord(a.w, x, y, z);
                            c.setAsVector(shadow);
                            Vec3 real = selected.shadow2real(shadow);
                            dest.set(real);
                            dest.markBlockForUpdate();
                        }
                    }
                }
                /*Coord realMin = new Coord(selected);
                Coord realMax = realMin.copy();
                realMin.set(selected.shadow2real(vShadowMin));
                realMax.set(selected.shadow2real(vShadowMax));
                selected.worldObj.markBlockRangeForRenderUpdate(realMin.x, realMin.y, realMin.z, realMax.x, realMax.y, realMax.z);*/
                selected.setDead();
                setSelection(null);
            }}, Requires.SELECTION);
        add(new SubCommand("spawn") {
            @Override
            void call(String[] args) {
                DimensionSliceEntity currentWE = Hammer.allocateSlice(user.w);
                user.setAsEntityLocation(currentWE);
                currentWE.worldObj.spawnEntityInWorld(currentWE);
                ((EntityPlayerMP) sender).addChatMessage("Created FZDS " + currentWE.cell);
                setSelection(currentWE);
            }}, Requires.COORD);
        add(new SubCommand("show") {
            @Override
            void call(String[] args) {
                int cell = 0;
                if (args.length > 0) {
                    cell = Integer.valueOf(args[0]);
                }
                DimensionSliceEntity currentWE = Hammer.spawnSlice(user.w, cell);
                user.setAsEntityLocation(currentWE);
                currentWE.worldObj.spawnEntityInWorld(currentWE);
                ((EntityPlayerMP) sender).addChatMessage("Showing FZDS " + currentWE.cell);
                setSelection(currentWE);
            }}, Requires.COORD);
        add(new SubCommand("grass") {			
            @Override
            void call(String[] args) {
                user.add(0, -1, 0).setId(Block.grass);
            }}, Requires.COORD, Requires.CREATIVE);
        add(new SubCommand("snap") {
            @Override
            void call(String[] args) {
                selected.posX = (int) selected.posX;
                selected.posY = (int) selected.posY;
                selected.posZ = (int) selected.posZ;
            }}, Requires.SELECTION);
        add(new SubCommand("removeall") {
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
            void call(String[] args) {
                sender.sendChatToPlayer("> " + selected);
                setSelection(selected);
            }}, Requires.SELECTION);
        add(new SubCommand("rot?") {
            @Override
            void call(String[] args) {
                sender.sendChatToPlayer("r = " + selected.rotation);
                sender.sendChatToPlayer("ω = " + selected.rotationalVelocity);
                if (!selected.can(Caps.ROTATE)) {
                    sender.sendChatToPlayer("(Does not have the ROTATE cap, so this is meaningless)");
                }
            }}, Requires.SELECTION);
        add(new SubCommand("+|-") {
            @Override
            void call(String[] args) {
                boolean add = arg0.equals("+");
                Iterator<DimensionSliceEntity> it = Hammer.getSlices(MinecraftServer.getServer().worldServerForDimension(0)).iterator();
                DimensionSliceEntity first = null, prev = null, next = null, last = null;
                boolean found_current = false;
                while (it.hasNext()) {
                    DimensionSliceEntity here = it.next();
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
            void call(String[] args) {
                selected.setDead();
                setSelection(null);
                sender.sendChatToPlayer("Made dead");
            }}, Requires.SELECTION);
        add(new SubCommand("force_cell_allocation_count", "newCount") {
            @Override
            void call(String[] args) {
                if (args.length != 1) {
                    throw new SyntaxErrorException();
                }
                int newCount = Integer.parseInt(args[0]);
                Hammer.instance.hammerInfo.setAllocationCount(newCount);
            }}, Requires.OP);
        add(new SubCommand("sr|sw", "angle°", "direction") {
            @Override
            void call(String[] args) {
                if (args.length != 2) {
                    throw new SyntaxErrorException();
                }
                if (!selected.can(Caps.ROTATE)) {
                    sender.sendChatToPlayer("Selection does not have the rotation cap");
                    return;
                }
                double theta = Math.toRadians(Double.parseDouble(args[0]));
                ForgeDirection dir;
                try {
                    dir = ForgeDirection.valueOf(args[1].toUpperCase());
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
                Quaternion toMod = derivative == 0 ? selected.rotation : selected.rotationalVelocity;
                toMod.update(Quaternion.getRotationQuaternion(theta, dir));
            }}, Requires.SELECTION);
        add(new SubCommand("d|v|r|w", "+|=", "[W=1]", "X", "Y", "Z") {
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
                if ((type == 'r' || type == 'w') && !selected.can(Caps.ROTATE)) {
                    sender.sendChatToPlayer("Selection does not have the ROTATE cap");
                    return;
                }
                if (args[0].equals("+")) {
                    if (type == 'd' || type == 's') {
                        selected.setPosition(selected.posX + x, selected.posY + y, selected.posZ + z);
                    } else if (type == 'v') {
                        selected.addVelocity(x/20, y/20, z/20);
                    } else if (type == 'r') {
                        selected.rotation.incrAdd(new Quaternion(w, x, y, z));
                    } else if (type == 'w') {
                        selected.rotationalVelocity.incrAdd(new Quaternion(w, x, y, z));
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
                        selected.rotation = (new Quaternion(w, x, y, z));
                    } else if (type == 'w') {
                        Quaternion omega = (new Quaternion(w, x, y, z));
                        selected.rotationalVelocity = omega;
                    } else {
                        sender.sendChatToPlayer("Not a command?");
                    }
                    selected.rotation.incrNormalize();
                    selected.rotationalVelocity.incrNormalize();
                } else {
                    sender.sendChatToPlayer("+ or =?");
                }
            }}, Requires.SELECTION);
        add(new SubCommand("dirty") {
            @Override
            void call(String[] args) {
                selected.rotationalVelocity.w *= -1;
                selected.rotation.w *= -1;
                selected.rotation.w += 0.1;
            }}, Requires.SELECTION);
    }

}
