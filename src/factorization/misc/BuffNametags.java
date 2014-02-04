package factorization.misc;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization.MessageType;

public class BuffNametags {
    @ForgeSubscribe(priority = EventPriority.LOW)
    public void buffedNametag(EntityInteractEvent event) {
        ItemStack is = event.entityPlayer.getHeldItem();
        if (is == null) return;
        if (is.getItem() != Item.nameTag) return;
        if (!(event.target instanceof EntityLiving)) return;
        final EntityLiving ent = (EntityLiving) event.target;
        final String origName = ent.getCustomNameTag();
        ent.tasks.addTask(0, new EntityAIBase() {
            boolean buffApplied = false; // Might not be necessary.
            
            @Override
            public boolean shouldExecute() {
                if (buffApplied) return false;
                buffApplied = true;
                if (ent.getCustomNameTag().equals(origName)) return false;
                float delta = 2*5;
                float origHealth = ent.getMaxHealth();
                float newMaxHealth = origHealth + delta;
                ent.getEntityAttribute(SharedMonsterAttributes.maxHealth).setAttribute(newMaxHealth);
                ent.heal(delta);
                
                String particleType = "heart";
                if (ent instanceof IMob) {
                    particleType = "smoke";
                }
                Packet packet = Core.network.entityPacket(ent, MessageType.EntityParticles, (byte) 8, particleType);
                Core.network.broadcastPacket(null, new Coord(ent), packet);
                
                return false;
            }
        });
    }
}