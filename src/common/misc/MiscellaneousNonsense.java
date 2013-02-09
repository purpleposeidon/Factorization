package factorization.misc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.packet.Packet;
import net.minecraftforge.common.DungeonHooks;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.PacketDispatcher;
import factorization.common.Core;

@Mod(modid = MiscellaneousNonsense.modId, name = MiscellaneousNonsense.name, version = Core.version, dependencies = "required-after: " + Core.modId)
@NetworkMod(clientSideRequired = true, packetHandler = MiscNet.class, channels = {MiscNet.channel})
public class MiscellaneousNonsense {
    public static final String modId = Core.modId + ".misc";
    public static final String name = "Factorization Miscellaneous Nonsense";
    public static MiscNet net;
    @SidedProxy(clientSide = "factorization.misc.MiscClientProxy", serverSide = "factorization.misc.MiscProxy")
    public static MiscProxy proxy;
    public static MiscellaneousNonsense instance;
    
    public MiscellaneousNonsense() {
        MiscellaneousNonsense.instance = this;
    }
    
    @PreInit
    public void setParent(FMLPreInitializationEvent event) {
        event.getModMetadata().parent = Core.modId;
        DungeonHooks.addDungeonMob("Creeper", 1);
        String THATS_SOME_VERY_NICE_SOURCE_CODE_YOU_HAVE_THERE[] = {
                "##  ##",
                "##  ##",
                "  ##  ",
                " #### ",
                " #  # "
        };
        //TODO: Play click sound when MC starts up
        //TODO: move in achievements fixes
        //TODO: Running key
        //TODO: Middle-click
        //TODO: Netherstar sandwich ;D
        //Chocolate bars, chocolate blocks, cheese blocks
    }
    
    @ServerStarting
    public void addCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new FogCommand());
    }
    
    static class FogCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "fog";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (!(sender instanceof EntityPlayerMP)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) sender;
            player.playerNetServerHandler.sendPacketToPlayer(instance.makePacket(args));
        }
        
        static List<String> fogCommands = Arrays.asList("far", "0", "normal", "1", "short", "2", "tiny", "3", "micro", "4", "microfog", "5", "other");
        static List<String> otherCommands = Arrays.asList("pauserender", "gc", "now", "about", "clear", "saycoords");
        
        @Override
        public List addTabCompletionOptions(ICommandSender sender, String[] args) {
            if (args.length == 1) {
                return fogCommands;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("other")) {
                return otherCommands;
            }
            return super.addTabCompletionOptions(sender, args);
        }
        
        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return sender instanceof EntityPlayerMP;
        }
        
        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }
    }
    
    public Packet makePacket(String items[]) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(outputStream);
            for (String i : items) {
                output.writeUTF(i);
            }
            output.flush();
            return PacketDispatcher.getPacket(net.channel, outputStream.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
