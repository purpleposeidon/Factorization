package factorization.docs;

import java.util.Map.Entry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class FluidViewer implements IDocGenerator {

    @Override
    public void process(AbstractTypesetter out, String arg) {
        for (Entry<String, Fluid> entry : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = entry.getKey();
            Fluid fluid = entry.getValue();
            out.append("\\seg \\nl \\nl");
            if (fluid.canBePlacedInWorld()) {
                ItemStack is = new ItemStack(fluid.getBlock());
                out.emitWord(new ItemWord(is));
                out.append(" ");
            } else {
                IIcon icon = fluid.getIcon();
                out.emitWord(new IconWord(null, icon, IconWord.BLOCK_TEXTURE));
                out.append(" ");
            }
            out.append(String.format("\\u{%s}", name));
            if (fluid.isGaseous()) {
                out.append("\\nl A gas");
            }
            if (!fluid.canBePlacedInWorld()) {
                out.append("\\nl Item-only");
            }
            out.append(String.format("\\nl Temperature: %s°K", fluid.getTemperature()));
            out.append(String.format("\\nl Density: %s kg/block", fluid.getDensity()));
            //out.append(String.format("\\nlViscoscity: %s", fluid.getViscosity()));
            
            out.append("\\endseg");
        }
    }

}
