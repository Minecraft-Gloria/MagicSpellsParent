package com.nisovin.magicspells.spells.targeted;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;

public class PotionEffectSpell extends TargetedSpell implements TargetedEntitySpell {

	private List<ConfigData<PotionEffect>> potionEffects;
	private List<?> potionEffectData;

	private final ConfigData<PotionEffectType> type;

	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> strength;

	private final ConfigData<Boolean> icon;
	private final ConfigData<Boolean> hidden;
	private final ConfigData<Boolean> ambient;
	private final ConfigData<Boolean> override;
	private final ConfigData<Boolean> targeted;
	private final boolean spellPowerAffectsDuration;
	private final boolean spellPowerAffectsStrength;

	public PotionEffectSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		potionEffectData = getConfigList("potion-effects", null);

		type = ConfigDataUtil.getPotionEffectType(config.getMainConfig(), "spells." + internalName + ".type", PotionEffectType.SPEED);

		duration = getConfigDataInt("duration", 0);
		strength = getConfigDataInt("strength", 0);

		icon = getConfigDataBoolean("icon", true);
		hidden = getConfigDataBoolean("hidden", false);
		ambient = getConfigDataBoolean("ambient", false);
		targeted = getConfigDataBoolean("targeted", false);
		override = getConfigDataBoolean("override", false);
		spellPowerAffectsDuration = getConfigBoolean("spell-power-affects-duration", true);
		spellPowerAffectsStrength = getConfigBoolean("spell-power-affects-strength", true);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (potionEffectData == null) return;

		potionEffects = new ArrayList<>();
		for (Object potionEffectObj : potionEffectData) {
			if (potionEffectObj instanceof String potionEffectString) {
				String[] data = potionEffectString.split(" ");
				if (data.length == 0) continue;

				PotionEffectType type = Util.getPotionEffectType(data[0]);
				if (type == null) {
					MagicSpells.error("Invalid potion effect string '" + potionEffectString + "' in PotionEffectSpell '" + internalName + "'.");
					continue;
				}

				int duration = 0;
				if (data.length >= 2) {
					try {
						duration = Integer.parseInt(data[1]);
					} catch (NumberFormatException e) {
						MagicSpells.error("Invalid duration '" + duration + "' in potion effect string '" + potionEffectString + "' in PotionEffectSpell '" + internalName + "'.");
						continue;
					}
				}

				int strength = 0;
				if (data.length >= 3) {
					try {
						strength = Integer.parseInt(data[2]);
					} catch (NumberFormatException e) {
						MagicSpells.error("Invalid strength '" + strength + "' in potion effect string '" + potionEffectString + "' in PotionEffectSpell '" + internalName + "'.");
						continue;
					}
				}

				boolean ambient = data.length >= 4 && Boolean.parseBoolean(data[4]);
				boolean particles = data.length < 5 || !Boolean.parseBoolean(data[3]);
				boolean icon = data.length < 6 || Boolean.parseBoolean(data[5]);

				if (data.length > 6)
					MagicSpells.error("Trailing data found in potion effect string '" + potionEffectString + "' in PotionEffectSpell '" + internalName + "'.");

				if (spellPowerAffectsDuration || spellPowerAffectsStrength) {
					int finalDuration = duration;
					int finalStrength = strength;

					ConfigData<PotionEffect> effect;
					if (!spellPowerAffectsStrength)
						effect = spellData -> new PotionEffect(type, Math.round(finalDuration * spellData.power()), finalStrength, ambient, particles, icon);
					else if (!spellPowerAffectsDuration)
						effect = spellData -> new PotionEffect(type, finalDuration, Math.round(finalStrength * spellData.power()), ambient, particles, icon);
					else
						effect = spellData -> new PotionEffect(type, Math.round(finalDuration * spellData.power()), Math.round(finalStrength * spellData.power()), ambient, particles, icon);

					potionEffects.add(effect);
				} else {
					PotionEffect effect = new PotionEffect(type, duration, strength, ambient, particles, icon);
					potionEffects.add(spellData -> effect);
				}
			} else if (potionEffectObj instanceof Map<?, ?> potionEffectMap) {
				ConfigurationSection section = ConfigReaderUtil.mapToSection(potionEffectMap);

				ConfigData<PotionEffectType> type = ConfigDataUtil.getPotionEffectType(section, "type", PotionEffectType.SPEED);
				ConfigData<Integer> duration = ConfigDataUtil.getInteger(section, "duration", 0);
				ConfigData<Integer> strength = ConfigDataUtil.getInteger(section, "strength", 0);
				ConfigData<Boolean> ambient = ConfigDataUtil.getBoolean(section, "ambient", false);
				ConfigData<Boolean> hidden = ConfigDataUtil.getBoolean(section, "hidden", false);
				ConfigData<Boolean> icon = ConfigDataUtil.getBoolean(section, "icon", true);

				ConfigData<PotionEffect> effect = spellData -> {
					int d = duration.get(spellData);
					if (spellPowerAffectsDuration) d = Math.round(d * spellData.power());

					int s = strength.get(spellData);
					if (spellPowerAffectsStrength) s = Math.round(s * spellData.power());

					return new PotionEffect(
						type.get(spellData),
						d,
						s,
						ambient.get(spellData),
						!hidden.get(spellData),
						icon.get(spellData)
					);
				};

				potionEffects.add(effect);
			}
		}

		if (potionEffects.isEmpty()) potionEffects = null;
		potionEffectData = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (targeted.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();
		} else {
			SpellTargetEvent targetEvent = new SpellTargetEvent(this, data, data.caster());
			if (!targetEvent.callEvent()) return noTarget(targetEvent);
			data = targetEvent.getSpellData();
		}

		return castAtEntity(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (potionEffects == null) {
			PotionEffectType type = this.type.get(data);

			int duration = this.duration.get(data);
			if (spellPowerAffectsDuration) duration = Math.round(duration * data.power());

			int strength = this.strength.get(data);
			if (spellPowerAffectsStrength) strength = Math.round(strength * data.power());

			boolean ambient = this.ambient.get(data);
			boolean particles = !this.hidden.get(data);
			boolean icon = this.icon.get(data);

			PotionEffect effect = new PotionEffect(type, duration, strength, ambient, particles, icon);

			callDamageEvent(data.caster(), data.target(), effect, data);

			if (override.get(data) && data.target().hasPotionEffect(type)) data.target().removePotionEffect(type);
			data.target().addPotionEffect(effect);

			playSpellEffects(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		for (ConfigData<PotionEffect> effectData : potionEffects) {
			PotionEffect effect = effectData.get(data);

			callDamageEvent(data.caster(), data.target(), effect, data);

			if (override.get(data) && data.target().hasPotionEffect(effect.getType())) data.target().removePotionEffect(effect.getType());
			data.target().addPotionEffect(effect);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void callDamageEvent(LivingEntity caster, LivingEntity target, PotionEffect effect, SpellData data) {
		DamageCause cause = null;
		if (effect.getType() == PotionEffectType.POISON) cause = DamageCause.POISON;
		else if (effect.getType() == PotionEffectType.WITHER) cause = DamageCause.WITHER;

		if (cause != null) new SpellApplyDamageEvent(this, caster, target, effect.getAmplifier(), cause, "", data).callEvent();
	}

}
