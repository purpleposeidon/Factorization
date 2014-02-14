package factorization.docs;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatMessageComponent;
import net.minecraftforge.client.ClientCommandHandler;

public class DocumentationModule {
    public static void processCommand(ICommandSender icommandsender, String[] astring) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new DocViewer("test")); // Doesn't actually work. In any case: NORELEASE, add an item
    }

    
}
