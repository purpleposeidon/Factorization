package factorization.truth.gen;

import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
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
            return new IconWord(icon, IconWord.BLOCK_TEXTURE);
        }
    }

    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        for (Entry<String, Fluid> entry : FluidRegistry.getRegisteredFluids().entrySet()) {
            String name = entry.getKey();
            Fluid fluid = entry.getValue();
            out.write("\\seg \\nl \\nl");
            out.write(convert(fluid));
            out.write(" ");
            out.write(String.format("\\u{%s}", name));
            if (fluid.isGaseous()) {
                out.write("\\nl A gas");
            }
            if (!fluid.canBePlacedInWorld()) {
                out.write("\\nl Item-only");
            }
            out.write(String.format("\\nl Temperature: %s°K", fluid.getTemperature()));
            out.write(String.format("\\nl Density: %s kg/block", fluid.getDensity()));
            //out.write(String.format("\\nlViscoscity: %s", fluid.getViscosity()));
            
            out.write("\\endseg");
        }
    }

}
