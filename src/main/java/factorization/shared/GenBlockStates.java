package factorization.shared;

import com.google.gson.stream.JsonWriter;
import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class GenBlockStates extends CommandBase {
    @Override
    public String getCommandName() {
        return "heap-block-states";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/heap-block-states while holding an Block";
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof EntityPlayer;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        EntityPlayer player = (EntityPlayer) sender;
        ItemStack held = player.getHeldItem();
        if (held == null) {
            sender.addChatMessage(new ChatComponentText("You aren't holding an item."));
            return;
        }
        if (args.length > 0) {
            Minecraft mc = Minecraft.getMinecraft();
            IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(held);
            return;
        }
        Block block = DataUtil.getBlock(held.getItem());
        if (block == null) {
            sender.addChatMessage(new ChatComponentText("That isn't a Block"));
            return;
        }
        String blockName = DataUtil.getName(block).split(":")[1];
        File name = new File("jsonOut/" + blockName + ".json");
        IBlockState bs = block.getDefaultState();
        // TODO: IExtendedBS?
        FileWriter writer;
        try {
            writer = new FileWriter(name);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        JsonWriter out = new JsonWriter(writer);
        out.setIndent("    ");
        try {
            writeBlockState(bs, out);
            out.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void writeBlockState(IBlockState bs, JsonWriter out) throws IOException {
        out.beginObject();
        out.name("variants");
        out.beginObject();
        ArrayList<IProperty> propertyCopy = new ArrayList<>(bs.getProperties().keySet());
        visit(out, propertyCopy, null);
        out.endObject();
        out.endObject();
    }

    void visit(JsonWriter out, ArrayList<IProperty> remainders, String prefix) throws IOException {
        if (remainders.isEmpty()) {
            if (prefix == null) {
                // Has no properties. I guess this is what to use?
                prefix = "normal";
            }

            out.name(prefix);
            out.beginObject();
            out.name("model");
            out.value("factorization:NORELEASE");
            out.endObject();
            return;
        }
        remainders = new ArrayList<>(remainders);
        IProperty prop = remainders.remove(0);
        if (prefix != null) {
            prefix += "," + prop.getName() + "=";
        } else {
            prefix = prop.getName() + "=";
        }
        for (Object x : prop.getAllowedValues()) {
            visit(out, remainders, prefix + x.toString());
        }
    }
}
