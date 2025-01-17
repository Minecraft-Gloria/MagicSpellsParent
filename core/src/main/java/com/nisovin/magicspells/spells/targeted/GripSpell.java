package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;

public class GripSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private final ConfigData<Double> yOffset;
	private final ConfigData<Double> locationOffset;

	private final ConfigData<Boolean> checkGround;

	private final ConfigData<Vector> relativeOffset;

	private String strCantGrip;

	public GripSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		yOffset = getConfigDataDouble("y-offset", 0);
		locationOffset = getConfigDataDouble("location-offset", 0);

		checkGround = getConfigDataBoolean("check-ground", true);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(1, 1, 0));

		strCantGrip = getConfigString("str-cant-grip", "");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntityFromLocation(info.spellData().location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return castAtEntityFromLocation(data.location(data.caster().getLocation()));
	}

	@Override
	public CastResult castAtEntityFromLocation(SpellData data) {
		Location loc = data.location();

		Vector startDir = loc.clone().getDirection().normalize();
		Vector horizOffset = new Vector(-startDir.getZ(), 0.0, startDir.getX()).normalize();

		Vector relativeOffset = this.relativeOffset.get(data);

		double yOffset = this.yOffset.get(data);
		if (yOffset != 0) relativeOffset.setY(yOffset);

		double locationOffset = this.locationOffset.get(data);
		if (locationOffset != 0) relativeOffset.setX(locationOffset);

		loc.add(horizOffset.multiply(relativeOffset.getZ()));
		loc.add(loc.getDirection().clone().multiply(relativeOffset.getX()));
		loc.setY(loc.getY() + relativeOffset.getY());
		data = data.location(loc);

		if (checkGround.get(data) && !BlockUtils.isPathable(loc.getBlock())) return noTarget(strCantGrip, data);

		data.target().teleportAsync(loc);
		playSpellEffects(data);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
