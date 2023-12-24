package com.nisovin.magicspells.util;

import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public record SpellData(LivingEntity caster, LivingEntity target, Location location, LivingEntity recipient,
						float power, String[] args, Object extra) {

	public static final SpellData NULL = new SpellData(null, null, null, null, 1f, null, null);

	public SpellData(LivingEntity caster, LivingEntity target, Location location, LivingEntity recipient, float power, String[] args, Object extra) {
		this.caster = caster;
		this.target = target;
		this.location = location == null ? null : location.clone();
		this.recipient = recipient;
		this.power = power;
		this.args = args;
		this.extra = extra;
	}

	public SpellData(LivingEntity caster, LivingEntity target, Location location, LivingEntity recipient, float power, String[] args) {
		this(caster, target, location, recipient, power, args, null);
	}

	public SpellData(LivingEntity caster, LivingEntity target, Location location, float power, String[] args) {
		this(caster, target, location, null, power, args);
	}

	public SpellData(LivingEntity caster, LivingEntity target, float power, String[] args) {
		this(caster, target, null, null, power, args);
	}

	public SpellData(LivingEntity caster, Location location, float power, String[] args) {
		this(caster, null, location, null, power, args);
	}

	public SpellData(LivingEntity caster, LivingEntity target, float power) {
		this(caster, target, null, null, power, null);
	}

	public SpellData(LivingEntity caster, Location location, float power) {
		this(caster, null, location, null, power, null);
	}

	public SpellData(LivingEntity caster, LivingEntity target) {
		this(caster, target, null, null, 1f, null);
	}

	public SpellData(LivingEntity caster, float power, String[] args) {
		this(caster, null, null, null, power, args);
	}

	public SpellData(LivingEntity caster, float power) {
		this(caster, null, null, null, power, null);
	}

	public SpellData(LivingEntity caster, Object extra) {
		this(caster, null, null, null, 1f, null, extra);
	}

	public SpellData(LivingEntity caster) {
		this(caster, null, null, null, 1f, null);
	}

	public Builder builder() {
		return new Builder(this);
	}

	public SpellData invert() {
		LivingEntity caster = this.caster;
		LivingEntity target = this.target;

		if (this.caster != null) target = this.caster;
		if (this.target != null) caster = this.target;

		return Objects.equals(caster, this.caster) && Objects.equals(target, this.target) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	@Override
	@UnknownNullability
	public Location location() {
		return location == null ? null : location.clone();
	}

	@Override
	public LivingEntity recipient() {
		return recipient == null ? caster : recipient;
	}

	public SpellData retarget(LivingEntity target, Location location) {
		return Objects.equals(this.target, target) && Objects.equals(this.location, location) ? this :
			new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData caster(LivingEntity caster) {
		return Objects.equals(this.caster, caster) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData target(LivingEntity target) {
		return Objects.equals(this.target, target) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData location(Location location) {
		return Objects.equals(this.location, location) ? this :
			new SpellData(caster, target, location == null ? null : location.clone(), recipient, power, args, extra);
	}

	public SpellData recipient(LivingEntity recipient) {
		return Objects.equals(this.recipient, recipient) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData power(float power) {
		return this.power == power ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData args(String[] args) {
		return Arrays.equals(this.args, args) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData extra(Object extra) {
		return Objects.equals(this.extra, extra) ? this : new SpellData(caster, target, location, recipient, power, args, extra);
	}

	public SpellData noTargeting() {
		return target == null && location == null ? this : new SpellData(caster, null, null, recipient, power, args, extra);
	}

	public SpellData noTarget() {
		return target == null ? this : new SpellData(caster, null, location, recipient, power, args, extra);
	}

	public SpellData noLocation() {
		return location == null ? this : new SpellData(caster, target, null, recipient, power, args, extra);
	}

	public boolean hasCaster() {
		return caster != null;
	}

	public boolean hasTarget() {
		return target != null;
	}

	public boolean hasLocation() {
		return location != null;
	}

	public boolean hasArgs() {
		return args != null && args.length > 0;
	}

	public static class Builder {

		private LivingEntity caster;
		private LivingEntity recipient;
		private LivingEntity target;
		private Location location;
		private float power;
		private String[] args;
		private Object extra;

		public Builder() {
			this(SpellData.NULL);
		}

		public Builder(SpellData data) {
			caster = data.caster;
			target = data.target;
			location = data.location;
			recipient = data.recipient;
			power = data.power;
			args = data.args;
			extra = data.extra;
		}

		public Builder caster(LivingEntity caster) {
			this.caster = caster;
			return this;
		}

		public Builder target(LivingEntity target) {
			this.target = target;
			return this;
		}

		public Builder location(Location location) {
			this.location = location;
			return this;
		}

		public Builder recipient(LivingEntity recipient) {
			this.recipient = recipient;
			return this;
		}

		public Builder power(float power) {
			this.power = power;
			return this;
		}

		public Builder args(String[] args) {
			this.args = args;
			return this;
		}

		public Builder extra(Object extra) {
			this.extra = extra;
			return this;
		}

		public SpellData build() {
			return new SpellData(caster, target, location, recipient, power, args, extra);
		}

	}

}
