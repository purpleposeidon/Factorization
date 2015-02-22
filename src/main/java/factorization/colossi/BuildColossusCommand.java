package factorization.colossi;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import factorization.api.Coord;
import factorization.shared.Core;

public class BuildColossusCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "build-colossus";
    }

    @Override
    public String getCommandUsage(ICommandSender player) {
        return "/build-colossus ([spam] SEED)|reload-masks";
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return super.canCommandSenderUseCommand(player);
    }

    @Override
    public void processCommand(ICommandSender player, String[] args) {
        if (args.length == 0) {
            player.addChatMessage(new ChatComponentText(getCommandUsage(player)));
            return;
        }
        if (args[0].equalsIgnoreCase("reload-masks")) {
            MaskLoader.reloadMasks();
            return;
        }
        ChunkCoordinates cc = player.getPlayerCoordinates();
        Coord at = new Coord(player.getEntityWorld(), cc.posX, cc.posY, cc.posZ);
        if (player.getCommandSenderName().startsWith("@")) {
            at = at.add(0, 6, 0);		    
        }
        if (args[0].equalsIgnoreCase("spam") || args[0].equals("$")) {
            int randSeed;
            if (args[0].equals("$")) {
                MaskLoader.reloadMasks();
                randSeed = player.getEntityWorld().rand.nextInt();
                Core.logInfo("seed: " + randSeed);
            } else {
                randSeed = Integer.parseInt(args[1]);
            }
            for (int i = 0; i < 10; i++) {
                ColossalBuilder cb = doGen(at, randSeed + i);
                at = at.add(0, 0, cb.get_width() + 4);
                if (i == 0) {
                    at = at.add(0, 0, 6); // Some help for the first guy?
                }
            }
        } else {
            int randSeed = Integer.parseInt(args[0]);
            doGen(at, randSeed);
        }
    }
    
    ColossalBuilder doGen(Coord at, int randSeed) {
        Coord signAt = at.copy();
        ColossalBuilder builder = new ColossalBuilder(randSeed, at);
        builder.construct();
        
        if (signAt.getTE(TileEntityCommandBlock.class) != null) {
            signAt.setIdMd(Blocks.standing_sign, 12, true);
            TileEntitySign sign = signAt.getTE(TileEntitySign.class);
            if (sign != null) {
                sign.signText[0] = "Colossus Seed";
                sign.signText[1] = "" + randSeed;
                signAt.markBlockForUpdate();
            }
        }
        return builder;
    }

}
