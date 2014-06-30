package factorization.docs;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.notify.Notice;
import factorization.shared.FzUtil;

final class FzdocSerialize implements ICommand {
    @Override
    public int compareTo(Object arg0) {
        if (arg0 instanceof ICommand) {
            ICommand other = (ICommand) arg0;
            return getCommandName().compareTo(other.getCommandName());
        }
        return 0;
    }

    @Override
    public String getCommandName() {
        return "fzdoc-figure";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/fzdoc-serialize generates an FZDoc \\figure command";
    }

    @Override
    public List getCommandAliases() { return null; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender icommandsender) {
        return icommandsender instanceof EntityPlayer && FzUtil.isPlayerOpped(icommandsender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender icommandsender, String[] astring) { return null; }

    @Override
    public boolean isUsernameIndex(String[] astring, int i) { return false; }

    @Override
    public void processCommand(ICommandSender icommandsender, String[] astring) {
        if (!(icommandsender instanceof EntityPlayer)) return;
        EntityPlayer player = (EntityPlayer) icommandsender;
        Coord peak = new Coord(player).add(ForgeDirection.DOWN);
        Block gold = Blocks.gold_block;
        if (!peak.is(gold)) {
            msg(player, "Not on a gold block");
            return;
        }
        int ySize = 0;
        Coord at = peak.copy();
        while (at.is(gold)) {
            at.adjust(ForgeDirection.DOWN);
            ySize--;
        }
        at.adjust(ForgeDirection.UP);
        Coord bottom = at.copy();
        int xSize = measure(bottom, ForgeDirection.EAST, ForgeDirection.WEST, gold);
        int zSize = measure(bottom, ForgeDirection.SOUTH, ForgeDirection.NORTH, gold);
        
        if (xSize*ySize*zSize == 0) {
            msg(player, "Invalid dimensions");
            return;
        }
        if (Math.abs(xSize) > 0xF) {
            msg(player, "X axis is too large");
            return;
        }
        if (Math.abs(ySize) > 0xF) {
            msg(player, "Y ayis is too large");
            return;
        }
        if (Math.abs(zSize) > 0xF) {
            msg(player, "Z azis is too large");
            return;
        }
        
        xSize -= 2*Math.signum(xSize);
        zSize -= 2*Math.signum(zSize);
        ySize += 1;
        peak.x += Math.signum(xSize);
        peak.z += Math.signum(zSize);
        Coord max = peak.copy();
        Coord min = peak.add(xSize, ySize, zSize);
        Coord.sort(min, max);
        new Notice(max, "max").sendToAll();
        new Notice(min, "min").sendToAll();
        DocWorld dw = copyChunkToWorld(min, max);
        NBTTagCompound worldTag = new NBTTagCompound();
        dw.writeToTag(worldTag);
        try {
            String encoded = DocumentationModule.encodeNBT(worldTag);
            String cmd = "\\figure{\n" + encoded + "}";
            cmd = cmd.replace("\0", "");
            System.out.println(cmd);
            FzUtil.copyStringToClipboard(cmd);
            msg(player, "\\figure command copied to the clipboard");
        } catch (Throwable t) {
            msg(player, "An error occured");
            t.printStackTrace();
        }
    }

    DocWorld copyChunkToWorld(final Coord min, final Coord max) {
        final DocWorld w = new DocWorld();
        final DeltaCoord start = new DeltaCoord(0, 0, 0); //size.add(maxSize.scale(-1)).scale(0.5);
        Coord.iterateCube(min, max, new ICoordFunction() { @Override public void handle(Coord here) {
            if (here.isAir()) return;
            DeltaCoord dc = here.difference(min).add(start);
            w.setIdMdTe(dc, here.getId(), here.getMd(), here.getTE());
        }});
        DeltaCoord d = max.difference(min);
        d.y /= 2; // The top always points up, so it can be pretty tall
        w.diagonal = (int) (d.magnitude() + 1);
        copyEntities(w, min, max);
        w.orig.set(min);
        return w;
    }

    void copyEntities(DocWorld dw20, Coord min, Coord max) {
        AxisAlignedBB ab = Coord.aabbFromRange(min, max);
        List<Entity> ents = min.w.getEntitiesWithinAABBExcludingEntity(null, ab);
        for (Entity ent : ents) {
            if (ent instanceof EntityPlayer) {
                continue; //??? We probably could get away with it...
            }
            dw20.addEntity(ent);
        }
    }

    void msg(ICommandSender player, String msg) {
        player.addChatMessage(new ChatComponentText(msg));
    }

    int measure(Coord bottom, ForgeDirection east, ForgeDirection west, Block gold) {
        Coord at = bottom.copy();
        ForgeDirection d = bottom.add(east).is(gold) ? east : west;
        int size = 0;
        while (at.is(gold)) {
            at.adjust(d);
            size++;
        }
        return size*(d.offsetX + d.offsetY + d.offsetZ);
    }
}