package factorization.truth.gen;

import factorization.truth.AbstractTypesetter;
import factorization.truth.api.IDocGenerator;
import factorization.truth.word.IconWord;
import factorization.truth.word.ItemWord;
import factorization.truth.word.Word;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import java.util.Map.Entry;

public class FluidViewer implements IDocGenerator {

    public static Word convert(Fluid fluid) {
        if (fluid.canBePlacedInWorld()) {
            ItemStack is = new ItemStack(fluid.getBlock());
            return new ItemWord(is);
        } else {
            IIcon icon = fluid.getIcon();
            return new IconWord(null, icon, IconWord.BLOCK_TEXTURE);
        }
    }

    @Override
    public void process(AbstractTypesetter out, String arg) {
        for (Entry<String, Fluid> entry : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = entry.getKey();
            Fluid fluid = entry.getValue();
            out.append("\\seg \\nl \\nl");
            out.emitWord(convert(fluid));
            out.append(" ");
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
