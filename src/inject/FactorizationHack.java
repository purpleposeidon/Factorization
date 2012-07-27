package net.minecraft.src;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * This class is for getting around retarded java package shit.
 */
public class FactorizationHack {

    static public DamageSource imp_bite = new DamageSource("imp bite").setDamageBypassesArmor();
    static public DamageSource acidBurn = new DamageSource("acid burn").setDamageBypassesArmor();

    static public IChunkProvider getChunkProvider(World world) {
        return world.chunkProvider;
    }

    static public void damageEntity(EntityLiving ent, DamageSource source, int i) {
        ent.damageEntity(source, i);
    }

    static public ItemStack loadItemStackFromDataInput(DataInput input) throws IOException {
        NBTTagCompound tag = new NBTTagCompound();
        tag.load(input);
        return ItemStack.loadItemStackFromNBT(tag);
    }

    static public void tagWrite(NBTTagCompound tag, DataOutputStream output) throws IOException {
        tag.write(output);
    }
}
