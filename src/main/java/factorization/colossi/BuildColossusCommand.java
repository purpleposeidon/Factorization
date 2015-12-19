package factorization.colossi;

import factorization.api.Coord;
import factorization.shared.Core;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;

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
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
            return;
        }
        if (args[0].equalsIgnoreCase("reload-masks")) {
            MaskLoader.reloadMasks();
            return;
        }
        Coord at = new Coord(sender.getEntityWorld(), sender.getPosition());
        if (sender.getName().startsWith("@")) {
            at = at.add(0, 6, 0);
        }
        if (args[0].equalsIgnoreCase("spam") || args[0].equals("$")) {
            int randSeed;
            if (args[0].equals("$")) {
                MaskLoader.reloadMasks();
                randSeed = sender.getEntityWorld().rand.nextInt();
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
            signAt.set(BlockStandingSign.ROTATION, 12, true);
            TileEntitySign sign = signAt.getTE(TileEntitySign.class);
            if (sign != null) {
                sign.signText[0] = new ChatComponentText("Colossus Seed");
                sign.signText[1] = new ChatComponentText("" + randSeed);
                signAt.markBlockForUpdate();
            }
        }
        return builder;
    }

}
