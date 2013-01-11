package factorization.fzds;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.fzds.api.IFzdsEntryControl;

public class FZDSCommand extends CommandBase {
    private static DimensionSliceEntity currentWE = null;
    @Override
    public String getCommandName() {
        return "fzds";
    }
    
    class DSTeleporter extends Teleporter {
        public DSTeleporter(WorldServer par1WorldServer) {
            super(par1WorldServer);
        }
        
        Coord destination;
        @Override
        public void placeInPortal(Entity player, double par2, double par4, double par6, float par8) {
            destination.x--;
            destination.moveToTopBlock();
            if (player.worldObj == DimensionManager.getWorld(Core.dimension_slice_dimid)) {
                destination.y = Math.min(Hammer.wallHeight, destination.y);
            }
            destination.setAsEntityLocation(player);
            /*Coord below = new Coord(player);
            below = below.add(0, -3, 0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Coord platform = below.add(dx, 0, dz);
                    if (platform.isAir()) {
                        platform.setId(Block.stone);
                    }
                }
            }*/
        }
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            //TODO: Non-shitty command interface
            sender.sendChatToPlayer("Player: spawn (show #) grass (go [#=0]) (goc [#=0]) leave");
            sender.sendChatToPlayer("Selected: selection + - remove (d|v +|= x y z)");
            sender.sendChatToPlayer("removeall force_cell_allocation_count kill_most_entities");
            return;
        }
        String cmd = args[0];
        if (sender instanceof EntityPlayerMP) {
            final EntityPlayerMP player = (EntityPlayerMP) sender;
            if (cmd.equalsIgnoreCase("spawn")) {
                currentWE = Hammer.allocateSlice(player.worldObj);
                currentWE.setPosition((int)player.posX, (int)player.posY, (int)player.posZ);
                currentWE.worldObj.spawnEntityInWorld(currentWE);
                ((EntityPlayerMP) sender).addChatMessage("Created FZDS " + currentWE.cell);
                return;
            }
            if (cmd.equalsIgnoreCase("show")) {
                int cell = 0;
                if (args.length == 2) {
                    cell = Integer.valueOf(args[1]);
                }
                currentWE = Hammer.spawnSlice(player.worldObj, cell);
                currentWE.setPosition((int)player.posX, (int)player.posY, (int)player.posZ);
                currentWE.worldObj.spawnEntityInWorld(currentWE);
                ((EntityPlayerMP) sender).addChatMessage("Showing FZDS " + currentWE.cell);
                return;
            }
            if (cmd.equalsIgnoreCase("grass")) {
                new Coord(player).add(0, -1, 0).setId(Block.grass);
                return;
            }
            ServerConfigurationManager manager = MinecraftServer.getServerConfigurationManager(MinecraftServer.getServer());
            DSTeleporter tp = new DSTeleporter((WorldServer) player.worldObj);
            tp.destination = new Coord (player.worldObj, 0, 0, 0);
            if (cmd.equalsIgnoreCase("go") || cmd.equalsIgnoreCase("goc")) {
                World hammerWorld = player.worldObj;
                int destinationCell = 0;
                if (args.length == 2) {
                    destinationCell = Integer.parseInt(args[1]);
                } 
                if (cmd.equalsIgnoreCase("goc")) {
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
                return;
            }
            if (cmd.equalsIgnoreCase("leave")) {
                if (DimensionManager.getWorld(0) != player.worldObj) {
                    ChunkCoordinates target = player.getBedLocation();
                    if (target != null) {
                        tp.destination.set(target);
                    }
                    manager.transferPlayerToDimension(player, 0, tp);
                }
                return;
            }
        }
        if (cmd.equals("removeall")) {
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
            return;
        }
        if (cmd.equals("selection")) {
            sender.sendChatToPlayer("> " + currentWE);
            return;
        }
        if (cmd.equals("+") || cmd.equals("-")) {
            boolean add = cmd.equals("+");
            Iterator<DimensionSliceEntity> it = Hammer.getSlices(MinecraftServer.getServer().worldServerForDimension(0)).iterator();
            DimensionSliceEntity first = null, prev = null, next = null, last = null;
            boolean found_current = false;
            while (it.hasNext()) {
                DimensionSliceEntity here = it.next();
                if (here.isDead) {
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
                if (last == currentWE) {
                    found_current = true;
                }
            }
            if (first == null) {
                sender.sendChatToPlayer("There are no DSEs loaded");
                return;
            }
            if (currentWE == null) {
                //initialize selection
                currentWE = add ? first : last;
            } else if (currentWE == last && add) {
                currentWE = first;
            } else if (currentWE == first && !add) {
                currentWE = last;
            } else {
                currentWE = add ? next : prev;
            }
            sender.sendChatToPlayer("> " + currentWE);
            return;
        }
        if (cmd.equalsIgnoreCase("remove")) {
            if (currentWE == null) {
                sender.sendChatToPlayer("No selection");
            } else {
                currentWE.setDead();
                currentWE = null;
                sender.sendChatToPlayer("Made dead");
            }
            return;
        }
        if (cmd.equals("force_cell_allocation_count")) {
            int newCount = Integer.parseInt(args[1]);
            Hammer.instance.hammerInfo.setAllocationCount(newCount);
            return;
        }
        if (cmd.equals("kill_most_entities")) {
            for (World w : MinecraftServer.getServer().worldServers) {
                for (Entity e : (Iterable<Entity>)w.loadedEntityList) {
                    if (e instanceof EntityPlayer) {
                        continue;
                    }
                    if (e instanceof DimensionSliceEntity || e instanceof IFzdsEntryControl) {
                        continue;
                    }
                    e.setDead();
                }
            }
            return;
        }
        if (cmd.equals("d") || cmd.equals("v")) {
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            if (args[1].equals("+")) {
                if (cmd.equals("d")) {
                    currentWE.posX += x;
                    currentWE.posY += y;
                    currentWE.posZ += z;
                } else {
                    currentWE.addVelocity(x, y, z);
                }
            } else {
                if (cmd.equals("d")) {
                    currentWE.setPosition(x, y, z);
                } else {
                    currentWE.setVelocity(x, y, z);
                }
            }
            return;
        }
        sender.sendChatToPlayer("Not a command");
    }
    
    

}
