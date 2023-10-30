package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.compat.CompatBasics;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.events.MagicSpellsEntityDamageByEntityEvent;

import java.math.BigDecimal;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<String> spellDamageType;
	private final ConfigData<DamageCause> damageType;

	private final ConfigData<Double> damage;

	private final ConfigData<Boolean> ignoreArmor;
	private final ConfigData<Boolean> checkPlugins;
	private final ConfigData<Boolean> powerAffectsDamage;
	private final ConfigData<Boolean> avoidDamageModification;
	private final ConfigData<Boolean> tryAvoidingAntiCheatPlugins;

	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		spellDamageType = getConfigDataString("spell-damage-type", "");

		damageType = getConfigDataEnum("damage-type", DamageCause.class, DamageCause.ENTITY_ATTACK);

		damage = getConfigDataDouble("damage", 4);

		ignoreArmor = getConfigDataBoolean("ignore-armor", false);
		checkPlugins = getConfigDataBoolean("check-plugins", true);
		powerAffectsDamage = getConfigDataBoolean("power-affects-damage", true);
		avoidDamageModification = getConfigDataBoolean("avoid-damage-modification", true);
		tryAvoidingAntiCheatPlugins = getConfigDataBoolean("try-avoiding-anticheat-plugins", false);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		if (data.caster() instanceof Player caster)
			return CompatBasics.exemptAction(() -> castAtEntity(info.spellData()), caster, CompatBasics.activeExemptionAssistant.getPainExemptions());

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!data.target().isValid()) return noTarget(data);

		double damage = this.damage.get(data);
		if (powerAffectsDamage.get(data)) damage *= data.power();

		DamageCause damageType = this.damageType.get(data);
		String spellDamageType = this.spellDamageType.get(data);

//		if (checkPlugins.get(data)) {
		MagicSpellsEntityDamageByEntityEvent damageEvent = new MagicSpellsEntityDamageByEntityEvent(data.caster(), data.target(), damageType, damage, this);
		if (!damageEvent.callEvent()) return noTarget(data);

		if (!avoidDamageModification.get(data)) damage = damageEvent.getDamage();
		data.target().setLastDamageCause(damageEvent);
//		}

		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, data.caster(), data.target(), damage, damageType, spellDamageType, data.args());
		event.callEvent();
		damage = event.getFinalDamage();

		if(!event.isProcessed()) {
//		if (ignoreArmor.get(data)) {
			double maxHealth = Util.getMaxHealth(data.target());

			double health = Math.min(data.target().getHealth(), maxHealth);
			health = Math.max(Math.min(BigDecimal.valueOf(health).subtract(BigDecimal.valueOf(damage)).doubleValue(), maxHealth), 0);

			if (health == 0 && data.caster() instanceof Player player) data.target().setKiller(player);
			data.target().setHealth(health);
			data.target().setLastDamage(damage);
//			Util.playHurtEffect(data.target(), data.caster());

//			playSpellEffects(data);
//			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

//		if (tryAvoidingAntiCheatPlugins.get(data)) data.target().damage(damage);
//		else data.target().damage(damage, data.caster());

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
