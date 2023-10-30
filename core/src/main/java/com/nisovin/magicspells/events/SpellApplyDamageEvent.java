package com.nisovin.magicspells.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.Spell;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SpellApplyDamageEvent extends SpellEvent {

	private final LivingEntity target;
	private double damage;
	private final String spellDamageType;
	private final DamageCause cause;
	private final long timestamp;
	private float modifier;
	private final String[] args;

	private boolean processed = false;

	public SpellApplyDamageEvent(Spell spell, LivingEntity caster, LivingEntity target, double damage, DamageCause cause, String spellDamageType, String... args) {
		super(spell, caster);

		this.target = target;
		this.spellDamageType = spellDamageType;
		this.damage = damage;
		this.cause = cause;

		timestamp = System.currentTimeMillis();

		modifier = 1.0f;

		this.args = args;
	}

	public void applyDamageModifier(float modifier) {
		this.modifier *= modifier;
	}

	public LivingEntity getTarget() {
		return target;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public DamageCause getCause() {
		return cause;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public float getDamageModifier() {
		return modifier;
	}

	public double getFinalDamage() {
		return BigDecimal.valueOf(damage)
				.multiply(BigDecimal.valueOf(modifier))
				.setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	public String[] getArgs() {
		return this.args;
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

















	public String getSpellDamageType() {
		return spellDamageType;
	}

}
