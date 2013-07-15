package factorization.common;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.util.DamageSource;
import net.minecraft.entity.EntityLiving;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;


public class FactorizationHack {
    //TODO: All this can be moved elsewhere
    static public ItemStack loadItemStackFromDataInput(DataInput input) throws IOException {
        return ItemStack.loadItemStackFromNBT((NBTTagCompound) NBTBase.readNamedTag(input));
    }

    static public void tagWrite(NBTTagCompound tag, DataOutputStream output) throws IOException {
        NBTBase.writeNamedTag(tag, output);
    }
}
