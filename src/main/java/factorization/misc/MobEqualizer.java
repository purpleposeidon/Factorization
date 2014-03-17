package factorization.misc;

import java.util.ArrayList;

import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;

public class MobEqualizer {
    @SubscribeEvent(priority = EventPriority.LOW)
    public void upgradeMob(LivingSpawnEvent.SpecialSpawn event) {
        if (event.world.difficultySetting.getDifficultyId() <= 1) {
            return;
        }
        if (!(event.entityLiving instanceof EntityMob)) {
            return;
        }
        EntityMob ent = (EntityMob) event.entityLiving;
        if (event.world.rand.nextInt(400) > event.world.difficultySetting.getDifficultyId()) {
            return;
        }
        EntityPlayer template = pickNearPlayer(event);
        if (template == null) {
            return;
        }
        int equipment_count = 0;
        ItemStack[] armorCopies = new ItemStack[4];
        ItemStack weaponCopy = null;
        if (event.entity instanceof IRangedAttackMob || event.world.rand.nextBoolean()) {
            for (int i = 0; i < 4; i++) {
                ItemStack is = template.getCurrentArmor(i);
                if (is != null && is.getItem().isValidArmor(is, 3 - i, ent)) {
                    armorCopies[i] = is.copy();
                    equipment_count++;
                }
                //It's okay to leave slots empty
            }
        }
        ArrayList<ItemStack> weapons = new ArrayList();
        if (!(ent instanceof IRangedAttackMob) || event.world.rand.nextBoolean()) {
            //float orig_damage = (float)ent.getAttributeInstanceForAttributeType__getEntityAttribute(SharedMonsterAttributes.attackDamage__attackDamage).getDamage__getAttributeValue();
            float orig_damage = (float)ent.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
            for (int i = 0; i < 9; i++) {
                ItemStack is = template.inventory.getStackInSlot(i);
                if (is == null) {
                    continue;
                }
                if (is.stackSize != 1 || is.getMaxStackSize() != 1) {
                    continue;
                }
                is = is.copy();
                EnumAction act = is.getItemUseAction();
                if (act != EnumAction.block && act != EnumAction.none && act != EnumAction.bow) {
                    continue;
                }
                ItemStack orig_weapon = ent.getHeldItem();
                ent.setCurrentItemOrArmor(0, is);
                //float f = (float)ent.getAttributeInstanceForAttributeType__getEntityAttribute(SharedMonsterAttributes.attackDamage__attackDamage).getDamage__getAttributeValue();
                float f = (float)ent.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue();
                ent.setCurrentItemOrArmor(0, orig_weapon);
                if (f <= orig_damage) {
                    continue;
                }
                weapons.add(is);
            }
        }
        if (!weapons.isEmpty()) {
            weaponCopy = weapons.get(event.world.rand.nextInt(weapons.size())).copy();
            equipment_count++;
        }
        if (equipment_count <= 0) {
            return;
        }
        
        event.setCanceled(true);
        //ent.initCreature__onSpawnWithEgg(null); // We need to cancel the event so that we can call this before the below happens
        ent.onSpawnWithEgg(null); // We need to cancel the event so that we can call this before the below happens
        if (!ent.canPickUpLoot()) {
            return;
        }
        ent.setCanPickUpLoot(false);
        if (weaponCopy != null) {
            ent.setCurrentItemOrArmor(0, weaponCopy);
        }
        for (int i = 0; i < 4; i++) {
            ent.setCurrentItemOrArmor(i + 1, armorCopies[i]);
        }
        for (int i = 0; i < 5; i++) {
            ent.setEquipmentDropChance(i, 0);
        }
    }

    private EntityPlayer pickNearPlayer(LivingSpawnEvent.SpecialSpawn event) {
        //See "Algorithm R (Reservoir sampling)" in "The Art of Computer Programming: Seminumerical Algorithms" by Donald Knuth, Chapter 3.4.2, page 144.
        double maxDistanceSq = Math.pow(16*8, 2);
        EntityPlayer secretary = null;
        int interviews = 0;
        for (EntityPlayer player : (Iterable<EntityPlayer>)event.world.playerEntities) {
            if (player.capabilities.isCreativeMode) {
                continue;
            }
            if (event.entity.getDistanceSqToEntity(player) > maxDistanceSq) {
                continue;
            }
            interviews++;
            int M = event.world.rand.nextInt(interviews) + 1 /* converts from [0,i-1] to [1, i] */;
            if (M <= 1 /* we need only 1 sample */) {
                secretary = player;
            }
        }
        return secretary;
    }
}