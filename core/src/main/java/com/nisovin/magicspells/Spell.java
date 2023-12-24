package com.nisovin.magicspells;

import de.slikey.effectlib.Effect;

import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.Multimap;
import com.google.common.collect.LinkedListMultimap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;
import org.bukkit.event.HandlerList;
import org.bukkit.event.EventHandler;
import org.bukkit.util.BlockIterator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventPriority;
import org.bukkit.block.data.BlockData;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.events.*;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.util.config.*;
import com.nisovin.magicspells.spelleffects.*;
import com.nisovin.magicspells.mana.ManaHandler;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.spells.PassiveSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spelleffects.effecttypes.*;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;
import com.nisovin.magicspells.util.magicitems.MagicItemDataParser;
import com.nisovin.magicspells.spelleffects.trackers.EffectTracker;
import com.nisovin.magicspells.spelleffects.util.EffectlibSpellEffect;
import com.nisovin.magicspells.spelleffects.trackers.AsyncEffectTracker;

public abstract class Spell implements Comparable<Spell>, Listener {

	protected static final Random random = ThreadLocalRandom.current();

	protected MagicConfig config;

	protected Map<UUID, Long> nextCast;
	protected Map<String, Integer> xpGranted;
	protected Map<String, Integer> xpRequired;
	protected Map<Spell, Float> sharedCooldowns;
	protected Map<String, Map<EffectPosition, List<Runnable>>> callbacks;

	protected Multimap<String, VariableMod> variableModsCast;
	protected Multimap<String, VariableMod> variableModsCasted;
	protected Multimap<String, VariableMod> variableModsTarget;

	protected IntMap<UUID> chargesConsumed;

	protected EnumMap<EffectPosition, List<SpellEffect>> effects;

	protected Set<String> tags;
	protected Set<CastItem> bindableItems;
	protected Set<Material> losTransparentBlocks;
	protected Set<EffectTracker> effectTrackerSet;
	protected Set<AsyncEffectTracker> asyncEffectTrackerSet;

	protected List<String> replaces;
	protected List<String> precludes;
	protected List<String> incantations;
	protected List<String> prerequisites;
	protected List<String> modifierStrings;
	protected List<String> worldRestrictions;
	protected List<String> rawSharedCooldowns;
	protected List<String> targetModifierStrings;
	protected List<String> locationModifierStrings;

	protected List<String> varModsCast;
	protected List<String> varModsCasted;
	protected List<String> varModsTarget;

	protected boolean debug;
	protected boolean obeyLos;
	protected boolean bindable;
	protected boolean beneficial;
	protected boolean helperSpell;
	protected boolean alwaysGranted;
	protected boolean castWithLeftClick;
	protected boolean castWithRightClick;
	protected boolean usePreciseCooldowns;
	protected boolean ignoreGlobalCooldown;
	protected boolean requireCastItemOnCommand;

	protected ConfigData<Boolean> targetSelf;
	protected ConfigData<Boolean> alwaysActivate;
	protected ConfigData<Boolean> interruptOnMove;
	protected ConfigData<Boolean> interruptOnCast;
	protected ConfigData<Boolean> playFizzleSound;
	protected ConfigData<Boolean> interruptOnDamage;
	protected ConfigData<Boolean> interruptOnTeleport;
	protected ConfigData<Boolean> spellPowerAffectsRange;

	protected CastItem[] castItems;
	protected CastItem[] consumeCastItems;
	protected CastItem[] leftClickCastItems;
	protected CastItem[] rightClickCastItems;

	protected String[] aliases;

	protected String name;
	protected String permName;
	protected String description;
	protected String strNoTarget;
	protected String internalName;
	protected String profilingKey;
	protected String strCastTarget;
	protected String rechargeSound;
	protected String soundOnCooldown;
	protected String spellNameOnFail;
	protected String danceCastSequence;
	protected String soundMissingReagents;
	protected String spellNameOnInterrupt;

	protected String strCost;
	protected String strCastSelf;
	protected String strCantCast;
	protected String strCantBind;
	protected String strCastStart;
	protected String strCastOthers;
	protected String strOnTeach;
	protected String strOnCooldown;
	protected String strWrongWorld;
	protected String strInterrupted;
	protected String strXpAutoLearned;
	protected String strWrongCastItem;
	protected String strModifierFailed;
	protected String strMissingReagents;

	protected ModifierSet modifiers;
	protected ModifierSet targetModifiers;
	protected ModifierSet locationModifiers;

	protected Subspell spellOnFail;
	protected Subspell spellOnInterrupt;

	protected SpellReagents reagents;

	protected ItemStack spellIcon;

	protected DamageCause targetDamageCause;

	protected ValidTargetList validTargetList;

	protected long nextCastServer;

	protected double targetDamageAmount;

	protected ConfigData<Integer> range;
	protected ConfigData<Integer> minRange;

	protected int charges;
	protected ConfigData<Integer> castTime;
	protected ConfigData<Integer> experience;
	protected ConfigData<Integer> broadcastRange;

	protected float cooldown;
	protected float serverCooldown;

	protected float minCooldown = -1F;
	protected float maxCooldown = -1F;

	protected final String internalKey;
	private final String className;

	private final ConfigurationSection defaultSection;

	public Spell(MagicConfig config, String spellName) {
		this.config = config;
		this.internalName = spellName;

		internalKey = "spells." + internalName + '.';
		className = getClass().getCanonicalName().replace("com.nisovin.magicspells.spells", "");

		Set<String> zoneNodes = config.getKeys("defaults");
		if (zoneNodes != null) defaultSection = config.getSection("defaults." + className);
		else defaultSection = null;

		callbacks = new HashMap<>();
		loadConfigData(config, spellName, "spells");
	}

	private void loadConfigData(MagicConfig config, String spellName, String section) {
		String path = section + '.' + spellName + '.';
		debug = config.getBoolean(path + "debug", false);
		name = config.getString(path + "name", spellName);
		profilingKey = "Spell:" + getClass().getName().replace("com.nisovin.magicspells.spells.", "") + '-' + spellName;
		List<String> temp = config.getStringList(path + "aliases", null);
		if (temp != null) {
			aliases = new String[temp.size()];
			aliases = temp.toArray(aliases);
		}
		helperSpell = config.getBoolean(path + "helper-spell", false);
		alwaysGranted = config.getBoolean(path + "always-granted", false);
		permName = config.getString(path + "permission-name", spellName);
		incantations = config.getStringList(path + "incantations", null);

		// General options
		description = config.getString(path + "description", "");
		if (config.contains(path + "cast-item")) {
			String[] sItems = config.getString(path + "cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			castItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid cast item specified: %i");
		} else if (config.contains(path + "cast-items")) {
			List<String> sItems = config.getStringList(path + "cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			castItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid cast item specified: %i");
		} else castItems = new CastItem[0];

		if (config.contains(path + "left-click-cast-item")) {
			String[] sItems = config.getString(path + "left-click-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			leftClickCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid left click cast item specified: %i");
		} else if (config.contains(path + "left-click-cast-items")) {
			List<String> sItems = config.getStringList(path + "left-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			leftClickCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid left click cast item listed: %i");
		} else leftClickCastItems = new CastItem[0];

		if (config.contains(path + "right-click-cast-item")) {
			String[] sItems = config.getString(path + "right-click-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			rightClickCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid right click cast item specified: %i");
		} else if (config.contains(path + "right-click-cast-items")) {
			List<String> sItems = config.getStringList(path + "right-click-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			rightClickCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid right click cast item listed: %i");
		} else rightClickCastItems = new CastItem[0];

		if (config.contains(path + "consume-cast-item")) {
			String[] sItems = config.getString(path + "consume-cast-item", "-5").trim().replace(" ", "").split(MagicItemDataParser.DATA_REGEX);
			consumeCastItems = setupCastItems(sItems, "Spell '" + internalName + "' has an invalid consume cast item specified: %i");
		} else if (config.contains(path + "consume-cast-items")) {
			List<String> sItems = config.getStringList(path + "consume-cast-items", null);
			if (sItems == null) sItems = new ArrayList<>();
			consumeCastItems = setupCastItems(sItems.toArray(new String[0]), "Spell '" + internalName + "' has an invalid consume cast item listed: %i");
		} else consumeCastItems = new CastItem[0];

		castWithLeftClick = config.getBoolean(path + "cast-with-left-click", MagicSpells.canCastWithLeftClick());
		castWithRightClick = config.getBoolean(path + "cast-with-right-click", MagicSpells.canCastWithRightClick());

		usePreciseCooldowns = config.getBoolean(path + "use-precise-cooldowns", false);

		danceCastSequence = config.getString(path + "dance-cast-sequence", null);
		requireCastItemOnCommand = config.getBoolean(path + "require-cast-item-on-command", false);
		bindable = config.getBoolean(path + "bindable", true);
		List<String> bindables = config.getStringList(path + "bindable-items", null);
		if (bindables != null) {
			bindableItems = new HashSet<>();
			for (String str : bindables) {
				MagicItem magicItem = MagicItems.getMagicItemFromString(str);
				if (magicItem == null) {
					MagicSpells.error("Spell '" + internalName + "' has an invalid bindable cast item specified: " + str);
					continue;
				}

				ItemStack item = magicItem.getItemStack();
				if (item == null) {
					MagicSpells.error("Spell '" + internalName + "' has an invalid bindable cast item specified: " + str);
					continue;
				}
				bindableItems.add(new CastItem(item));
			}
		}
		String iconStr = config.getString(path + "spell-icon", null);
		if (iconStr != null) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(iconStr);
			if (magicItem != null) {
				spellIcon = magicItem.getItemStack();
				if (spellIcon != null && !BlockUtils.isAir(spellIcon.getType())) {
					spellIcon.setAmount(0);
					if (!iconStr.contains("|")) {
						ItemMeta iconMeta = spellIcon.getItemMeta();
						iconMeta.displayName(Component.text(MagicSpells.getTextColor() + name));
						spellIcon.setItemMeta(iconMeta);
					}
				}
			}
		} else spellIcon = null;

		experience = getConfigDataInt("experience", 0);
		broadcastRange = getConfigDataInt("broadcast-range", MagicSpells.getBroadcastRange());

		// Cast time
		castTime = getConfigDataInt("cast-time", 0);
		interruptOnMove = getConfigDataBoolean("interrupt-on-move", true);
		interruptOnCast = getConfigDataBoolean("interrupt-on-cast", true);
		interruptOnDamage = getConfigDataBoolean("interrupt-on-damage", false);
		interruptOnTeleport = getConfigDataBoolean("interrupt-on-teleport", true);
		spellNameOnInterrupt = config.getString(path + "spell-on-interrupt", null);

		// Targeting
		minRange = getConfigDataInt("min-range", 0);
		range = getConfigDataInt("range", 20);
		spellPowerAffectsRange = getConfigDataBoolean("spell-power-affects-range", false);
		obeyLos = config.getBoolean(path + "obey-los", true);
		if (config.contains(path + "can-target")) {
			if (config.isList(path + "can-target"))
				validTargetList = new ValidTargetList(this, config.getStringList(path + "can-target", null));
			else validTargetList = new ValidTargetList(this, config.getString(path + "can-target", ""));
		} else {
			boolean targetPlayers = config.getBoolean(path + "target-players", true);
			boolean targetNonPlayers = config.getBoolean(path + "target-non-players", true);
			validTargetList = new ValidTargetList(targetPlayers, targetNonPlayers);
		}
		beneficial = config.getBoolean(path + "beneficial", isBeneficialDefault());
		targetDamageCause = null;
		String causeStr = config.getString(path + "target-damage-cause", null);
		if (causeStr != null) {
			for (DamageCause cause : DamageCause.values()) {
				if (!cause.name().equalsIgnoreCase(causeStr)) continue;
				targetDamageCause = cause;
				break;
			}
		}
		targetDamageAmount = config.getDouble(path + "target-damage-amount", 0);
		losTransparentBlocks = MagicSpells.getTransparentBlocks();
		if (config.contains(path + "los-transparent-blocks")) {
			losTransparentBlocks = Util.getMaterialList(config.getStringList(path + "los-transparent-blocks", Collections.emptyList()), HashSet::new);
			losTransparentBlocks.add(Material.AIR);
			losTransparentBlocks.add(Material.CAVE_AIR);
			losTransparentBlocks.add(Material.VOID_AIR);
		}
		targetSelf = getConfigDataBoolean("target-self", false);
		alwaysActivate = getConfigDataBoolean("always-activate", false);
		playFizzleSound = getConfigDataBoolean("play-fizzle-sound", false);
		strNoTarget = getConfigString("str-no-target", "");
		strCastTarget = getConfigString("str-cast-target", "");
		spellNameOnFail = getConfigString("spell-on-fail", "");

		// Cooldowns
		String cooldownRange = config.getString(path + "cooldown", "0");
		try {
			cooldown = Float.parseFloat(cooldownRange);
		} catch (NumberFormatException e) {

			// parse min max cooldowns
			String[] cdRange = cooldownRange.split("-");

			try {
				float min = Float.parseFloat(cdRange[0]);
				float max = Float.parseFloat(cdRange[1]);
				if (min > max) {
					minCooldown = max;
					maxCooldown = min;
				} else {
					minCooldown = min;
					maxCooldown = max;
				}
			} catch (NumberFormatException ignored) {
			}
		}

		serverCooldown = (float) config.getDouble(path + "server-cooldown", 0);
		rawSharedCooldowns = config.getStringList(path + "shared-cooldowns", null);
		ignoreGlobalCooldown = config.getBoolean(path + "ignore-global-cooldown", false);
		charges = config.getInt(path + "charges", 0);
		rechargeSound = config.getString(path + "recharge-sound", "");
		nextCast = new WeakHashMap<>();
		chargesConsumed = new IntMap<>();
		nextCastServer = 0;

		// Modifiers
		modifierStrings = config.getStringList(path + "modifiers", null);
		targetModifierStrings = config.getStringList(path + "target-modifiers", null);
		locationModifierStrings = config.getStringList(path + "location-modifiers", null);

		// Variables
		varModsCast = config.getStringList(path + "variable-mods-cast", null);
		varModsCasted = config.getStringList(path + "variable-mods-casted", null);
		varModsTarget = config.getStringList(path + "variable-mods-target", null);

		// Hierarchy options
		prerequisites = config.getStringList(path + "prerequisites", null);
		replaces = config.getStringList(path + "replaces", null);
		precludes = config.getStringList(path + "precludes", null);
		worldRestrictions = config.getStringList(path + "restrict-to-worlds", null);
		List<String> sXpGranted = config.getStringList(path + "xp-granted", null);
		List<String> sXpRequired = config.getStringList(path + "xp-required", null);
		if (sXpGranted != null) {
			xpGranted = new LinkedHashMap<>();
			for (String s : sXpGranted) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					xpGranted.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-granted entry for spell '" + internalName + "': " + s);
				}
			}
		}
		if (sXpRequired != null) {
			xpRequired = new LinkedHashMap<>();
			for (String s : sXpRequired) {
				String[] split = s.split(" ");
				try {
					int amt = Integer.parseInt(split[1]);
					xpRequired.put(split[0], amt);
				} catch (NumberFormatException e) {
					MagicSpells.error("Error in xp-required entry for spell '" + internalName + "': " + s);
				}
			}
		}

		soundOnCooldown = config.getString(path + "sound-on-cooldown", MagicSpells.getCooldownSound());
		soundMissingReagents = config.getString(path + "sound-missing-reagents", MagicSpells.getMissingReagentsSound());
		if (soundOnCooldown != null && soundOnCooldown.isEmpty()) soundOnCooldown = null;
		if (soundMissingReagents != null && soundMissingReagents.isEmpty()) soundMissingReagents = null;

		// Strings
		strCost = config.getString(path + "str-cost", null);
		strCantCast = config.getString(path + "str-cant-cast", MagicSpells.getCantCastMessage());
		strCantBind = config.getString(path + "str-cant-bind", null);
		strCastSelf = config.getString(path + "str-cast-self", null);
		strCastStart = config.getString(path + "str-cast-start", null);
		strCastOthers = config.getString(path + "str-cast-others", null);
		strOnTeach = config.getString(path + "str-on-teach", null);
		strOnCooldown = config.getString(path + "str-on-cooldown", MagicSpells.getOnCooldownMessage());
		strWrongWorld = config.getString(path + "str-wrong-world", MagicSpells.getWrongWorldMessage());
		strInterrupted = config.getString(path + "str-interrupted", null);
		strXpAutoLearned = config.getString(path + "str-xp-auto-learned", MagicSpells.getXpAutoLearnedMessage());
		strWrongCastItem = config.getString(path + "str-wrong-cast-item", strCantCast);
		strModifierFailed = config.getString(path + "str-modifier-failed", null);
		strMissingReagents = config.getString(path + "str-missing-reagents", MagicSpells.getMissingReagentsMessage());
		if (strXpAutoLearned != null) strXpAutoLearned = strXpAutoLearned.replace("%s", name);

		tags = new HashSet<>(config.getStringList(path + "tags", new ArrayList<>()));
		tags.add("spell-class:" + getClass().getCanonicalName());
		tags.add("spell-package:" + getClass().getPackage().getName());
	}

	public Set<String> getTags() {
		return Collections.unmodifiableSet(tags);
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	public String getLoggingSpellPrefix() {
		return '[' + internalName + ']';
	}

	protected SpellReagents getConfigReagents(String option) {
		List<String> costList = config.getStringList("spells." + internalName + '.' + option, null);
		if (costList == null || costList.isEmpty()) return null;

		SpellReagents reagents = new SpellReagents();
		String[] data;

		for (String costVal : costList) {
			try {
				// Parse cost data
				data = costVal.split(" ");

				switch (data[0].toLowerCase()) {
					case "health" -> {
						if (data.length > 1) reagents.setHealth(Double.parseDouble(data[1]));
					}
					case "mana" -> {
						if (data.length > 1) reagents.setMana(Integer.parseInt(data[1]));
					}
					case "hunger" -> {
						if (data.length > 1) reagents.setHunger(Integer.parseInt(data[1]));
					}
					case "experience" -> {
						if (data.length > 1) reagents.setExperience(Integer.parseInt(data[1]));
					}
					case "levels" -> {
						if (data.length > 1) reagents.setLevels(Integer.parseInt(data[1]));
					}
					case "durability" -> {
						if (data.length > 1) reagents.setDurability(Integer.parseInt(data[1]));
					}
					case "money" -> {
						if (data.length > 1) reagents.setMoney(Float.parseFloat(data[1]));
					}
					case "variable" -> {
						if (data.length > 2) reagents.addVariable(data[1], Double.parseDouble(data[2]));
					}

					default -> {
						int amount = 1;
						if (data.length > 1) amount = Integer.parseInt(data[1]);
						MagicItemData itemData = MagicItems.getMagicItemDataFromString(data[0]);
						if (itemData == null) {
							MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
							continue;
						}
						reagents.addItem(new SpellReagents.ReagentItem(itemData, amount));
					}
				}
			} catch (Exception e) {
				MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
			}
		}

		return reagents;
	}

	protected void initializeVariables() {
		// Variable options
		if (varModsCast != null && !varModsCast.isEmpty()) {
			variableModsCast = LinkedListMultimap.create();
			for (String s : varModsCast) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCast.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-cast option for spell '" + internalName + "': " + s);
				}
			}
		}
		if (varModsCasted != null && !varModsCasted.isEmpty()) {
			variableModsCasted = LinkedListMultimap.create();
			for (String s : varModsCasted) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsCasted.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-casted option for spell '" + internalName + "': " + s);
				}
			}
		}
		if (varModsTarget != null && !varModsTarget.isEmpty()) {
			variableModsTarget = LinkedListMultimap.create();
			for (String s : varModsTarget) {
				try {
					String[] data = s.split(" ", 2);
					String var = data[0];
					VariableMod varMod = new VariableMod(data[1]);
					variableModsTarget.put(var, varMod);
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-target option for spell '" + internalName + "': " + s);
				}
			}
		}

		// Cost
		reagents = getConfigReagents("cost");
		if (reagents == null) reagents = new SpellReagents();
	}

	protected void initializeSpellEffects() {
		// Graphical effects
		String path = "spells" + '.' + internalName + '.';
		effectTrackerSet = new HashSet<>();
		asyncEffectTrackerSet = new HashSet<>();
		if (!config.contains(path + "effects")) return;

		effects = new EnumMap<>(EffectPosition.class);

		if (!config.isSection(path + "effects")) return;
		for (String key : config.getKeys(path + "effects")) {
			ConfigurationSection section = config.getSection(path + "effects." + key);
			if (section == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a configuration section.");
				continue;
			}

			String positionName = section.getString("position", "");
			if (positionName.isEmpty()) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain a 'position' value.");
				continue;
			}

			EffectPosition position = EffectPosition.getPositionFromString(positionName);
			if (position == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'position' defined: " + positionName);
				continue;
			}

			String effectType = section.getString("effect", "");
			if (effectType.isEmpty()) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not contain an 'effect' value.");
				continue;
			}

			SpellEffect effect = MagicSpells.getSpellEffectManager().getSpellEffectByName(effectType);
			if (effect == null) {
				MagicSpells.error("Spell effect '" + key + "' on spell '" + internalName + "' does not have a valid 'effect' defined: " + effectType);
				continue;
			}

			effect.loadFromConfiguration(section);

			List<SpellEffect> effectList = effects.computeIfAbsent(position, p -> new ArrayList<>());
			effectList.add(effect);
		}
	}

	// DEBUG INFO: level 2, adding modifiers to internalname
	// DEBUG INFO: level 2, adding target modifiers to internalname
	protected void initializeModifiers() {
		// Modifiers
		if (modifierStrings != null && !modifierStrings.isEmpty()) {
			debug(2, "Adding modifiers to " + internalName + " spell");
			modifiers = new ModifierSet(modifierStrings, this);
			modifierStrings = null;
		}
		if (targetModifierStrings != null && !targetModifierStrings.isEmpty()) {
			debug(2, "Adding target modifiers to " + internalName + " spell");
			targetModifiers = new ModifierSet(targetModifierStrings, this);
			targetModifierStrings = null;
		}
		if (locationModifierStrings != null && !locationModifierStrings.isEmpty()) {
			debug(2, "Adding location modifiers to " + internalName + " spell");
			locationModifiers = new ModifierSet(locationModifierStrings, this);
			locationModifierStrings = null;
		}

		if (effects != null && !effects.isEmpty()) {
			for (EffectPosition position : effects.keySet()) {
				if (position == null) continue;

				List<SpellEffect> spellEffects = effects.get(position);
				if (spellEffects == null || spellEffects.isEmpty()) continue;
				spellEffects.forEach(spellEffect -> spellEffect.initializeModifiers(this));
			}
		}
	}

	/**
	 * This method is called immediately after all spells have been loaded.
	 */
	protected void initialize() {
		// Process shared cooldowns
		if (rawSharedCooldowns != null) {
			sharedCooldowns = new HashMap<>();
			for (String s : rawSharedCooldowns) {
				String[] data = s.split(" ");
				Spell spell = MagicSpells.getSpellByInternalName(data[0]);
				float cd = Float.parseFloat(data[1]);
				if (spell != null) sharedCooldowns.put(spell, cd);
			}
			rawSharedCooldowns.clear();
			rawSharedCooldowns = null;
		}

		// Register events
		registerEvents();

		// Other processing
		if (spellNameOnFail != null && !spellNameOnFail.isEmpty())
			spellOnFail = initSubspell(spellNameOnFail, "Spell '" + internalName + "' has an invalid spell-on-fail defined!");

		if (spellNameOnInterrupt != null && !spellNameOnInterrupt.isEmpty()) {
			spellOnInterrupt = initSubspell(spellNameOnInterrupt, "Spell '" + internalName + "' has an invalid spell-on-interrupt defined!");
		}
	}

	protected boolean configKeyExists(String key) {
		return config.contains(internalKey + key);
	}

	/**
	 * Access an integer config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected int getConfigInt(String key, int def) {
		return config.getInt(internalKey + key, getDefaultInt(key, def));
	}

	/**
	 * Access a long config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected long getConfigLong(String key, long def) {
		return config.getLong(internalKey + key, getDefaultLong(key, def));
	}

	/**
	 * Access a boolean config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected boolean getConfigBoolean(String key, boolean def) {
		return config.getBoolean(internalKey + key, getDefaultBoolean(key, def));
	}

	/**
	 * Access a String config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected String getConfigString(String key, String def) {
		return config.getString(internalKey + key, getDefaultString(key, def));
	}

	/**
	 * Access a Vector config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected Vector getConfigVector(String key, String def) {
		String[] vecStrings = getConfigString(key, getDefaultString(key, def)).split(",");
		return new Vector(Double.parseDouble(vecStrings[0]), Double.parseDouble(vecStrings[1]), Double.parseDouble(vecStrings[2]));
	}

	/**
	 * Access a float config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected float getConfigFloat(String key, float def) {
		return (float) config.getDouble(internalKey + key, getDefaultFloat(key, def));
	}

	/**
	 * Access a double config value for this spell.
	 *
	 * @param key The key of the config value
	 * @param def The value to return if it does not exist in the config
	 * @return The config value, or defaultValue if it does not exist
	 */
	protected double getConfigDouble(String key, double def) {
		return config.getDouble(internalKey + key, getDefaultDouble(key, def));
	}

	protected List<?> getConfigList(String key, List<?> def) {
		return config.getList(internalKey + key, getDefaultList(key, def));
	}

	protected List<Integer> getConfigIntList(String key, List<Integer> def) {
		return config.getIntList(internalKey + key, getDefaultIntList(key, def));
	}

	protected List<String> getConfigStringList(String key, List<String> def) {
		return config.getStringList(internalKey + key, getDefaultStringList(key, def));
	}

	protected Set<String> getConfigKeys(String key) {
		return config.getKeys(internalKey + key);
	}

	protected ConfigurationSection getConfigSection(String key) {
		return config.getSection(internalKey + key);
	}

	protected ConfigData<Boolean> getConfigDataBoolean(String key, boolean def) {
		return ConfigDataUtil.getBoolean(config.getMainConfig(), internalKey + key, getDefaultBoolean(key, def));
	}

	protected ConfigData<Boolean> getConfigDataBoolean(String key, ConfigData<Boolean> def) {
		return ConfigDataUtil.getBoolean(config.getMainConfig(), internalKey + key, getDefaultBoolean(key, def));
	}

	protected ConfigData<Integer> getConfigDataInt(String key, int def) {
		return ConfigDataUtil.getInteger(config.getMainConfig(), internalKey + key, getDefaultInt(key, def));
	}

	protected ConfigData<Integer> getConfigDataInt(String key, ConfigData<Integer> def) {
		return ConfigDataUtil.getInteger(config.getMainConfig(), internalKey + key, getDefaultInt(key, def));
	}

	protected ConfigData<Double> getConfigDataDouble(String key, double def) {
		return ConfigDataUtil.getDouble(config.getMainConfig(), internalKey + key, getDefaultDouble(key, def));
	}

	protected ConfigData<Double> getConfigDataDouble(String key, ConfigData<Double> def) {
		return ConfigDataUtil.getDouble(config.getMainConfig(), internalKey + key, getDefaultDouble(key, def));
	}

	protected ConfigData<Float> getConfigDataFloat(String key, float def) {
		return ConfigDataUtil.getFloat(config.getMainConfig(), internalKey + key, getDefaultFloat(key, def));
	}

	protected ConfigData<Float> getConfigDataFloat(String key, ConfigData<Float> def) {
		return ConfigDataUtil.getFloat(config.getMainConfig(), internalKey + key, getDefaultFloat(key, def));
	}

	protected ConfigData<Long> getConfigDataLong(String key, long def) {
		return ConfigDataUtil.getLong(config.getMainConfig(), internalKey + key, getDefaultLong(key, def));
	}

	protected ConfigData<Long> getConfigDataLong(String key, ConfigData<Long> def) {
		return ConfigDataUtil.getLong(config.getMainConfig(), internalKey + key, getDefaultLong(key, def));
	}

	protected ConfigData<String> getConfigDataString(String key, String def) {
		return ConfigDataUtil.getString(config.getMainConfig(), internalKey + key, getDefaultString(key, def));
	}

	protected ConfigData<Component> getConfigDataComponent(String key, Component def) {
		return ConfigDataUtil.getComponent(config.getMainConfig(), "spells." + internalName + '.' + key, def);
	}

	protected <T extends Enum<T>> ConfigData<T> getConfigDataEnum(String key, Class<T> type, T def) {
		return ConfigDataUtil.getEnum(config.getMainConfig(), internalKey + key, type, getDefaultEnum(key, type, def));
	}

	protected ConfigData<Vector> getConfigDataVector(String key, Vector def) {
		return ConfigDataUtil.getVector(config.getMainConfig(), "spells." + internalName + '.' + key, def);
	}

	public ConfigData<Material> getConfigDataMaterial(String key, Material def) {
		return ConfigDataUtil.getMaterial(config.getMainConfig(), "spells." + internalName + '.' + key, getDefaultEnum(key, Material.class, def));
	}

	public ConfigData<TargetBooleanState> getConfigDataTargetBooleanState(String key, TargetBooleanState def) {
		return ConfigDataUtil.getTargetBooleanState(config.getMainConfig(), "spells." + internalName + '.' + key, getDefaultEnum(key, TargetBooleanState.class, def));
	}

	public ConfigData<EntityType> getConfigDataEntityType(String key, EntityType def) {
		return ConfigDataUtil.getEntityType(config.getMainConfig(), "spells." + internalName + '.' + key, getDefaultEnum(key, EntityType.class, def));
	}

	protected ConfigData<BlockData> getConfigDataBlockData(String key, BlockData def) {
		return ConfigDataUtil.getBlockData(config.getMainConfig(), internalKey + key, getDefaultBlockData(key, def));
	}

	/**
	 * @param key Path for the keys to be read from. If the path is set to something like "filter", the keys will
	 *            be read from spell config section under the "filter" section.
	 */
	protected SpellFilter getConfigSpellFilter(String key) {
		return SpellFilter.fromConfig(config.getMainConfig(), internalKey + key);
	}

	/**
	 * Gets the config spell filter under the base path of the spell config.
	 */
	protected SpellFilter getConfigSpellFilter() {
		return getConfigSpellFilter("");
	}

	protected boolean isConfigString(String key) {
		return config.isString(internalKey + key);
	}

	protected boolean isConfigSection(String key) {
		return config.isSection(internalKey + key);
	}

	private int getDefaultInt(String key, int def) {
		if (defaultSection == null) return def;
		return defaultSection.getInt(key, def);
	}

	private long getDefaultLong(String key, long def) {
		if (defaultSection == null) return def;
		return defaultSection.getLong(key, def);
	}

	private boolean getDefaultBoolean(String key, boolean def) {
		if (defaultSection == null) return def;
		return defaultSection.getBoolean(key, def);
	}

	private String getDefaultString(String key, String def) {
		if (defaultSection == null) return def;
		return defaultSection.getString(key, def);
	}

	private float getDefaultFloat(String key, float def) {
		if (defaultSection == null) return def;
		return (float) defaultSection.getDouble(key, def);
	}

	private double getDefaultDouble(String key, double def) {
		if (defaultSection == null) return def;
		return defaultSection.getDouble(key, def);
	}

	private ConfigData<Boolean> getDefaultBoolean(String key, ConfigData<Boolean> def) {
		if (defaultSection == null) return def;
		return ConfigDataUtil.getBoolean(defaultSection, key, def);
	}

	private ConfigData<Integer> getDefaultInt(String key, ConfigData<Integer> def) {
		if (defaultSection == null) return def;
		return ConfigDataUtil.getInteger(defaultSection, key, def);
	}

	private ConfigData<Double> getDefaultDouble(String key, ConfigData<Double> def) {
		if (defaultSection == null) return def;
		return ConfigDataUtil.getDouble(defaultSection, key, def);
	}

	private ConfigData<Float> getDefaultFloat(String key, ConfigData<Float> def) {
		if (defaultSection == null) return def;
		return ConfigDataUtil.getFloat(defaultSection, key, def);
	}

	private ConfigData<Long> getDefaultLong(String key, ConfigData<Long> def) {
		if (defaultSection == null) return def;
		return ConfigDataUtil.getLong(defaultSection, key, def);
	}

	private <T extends Enum<T>> T getDefaultEnum(String key, Class<T> type, T def) {
		if (defaultSection == null) return def;
		String value = defaultSection.getString(key);
		if (value == null) return def;

		try {
			return Enum.valueOf(type, value.toUpperCase());
		} catch (IllegalArgumentException e) {
			return def;
		}
	}

	private BlockData getDefaultBlockData(String key, BlockData def) {
		if (defaultSection == null) return def;
		String value = defaultSection.getString(key);
		if (value == null) return def;

		try {
			return Bukkit.createBlockData(value.trim().toLowerCase());
		} catch (IllegalArgumentException e) {
			return def;
		}
	}

	private List<?> getDefaultList(String key, List<?> def) {
		if (defaultSection == null) return def;
		return defaultSection.getList(key, def);
	}

	private List<Integer> getDefaultIntList(String key, List<Integer> def) {
		if (defaultSection == null || !defaultSection.contains(key, true)) return def;
		return defaultSection.getIntegerList(key);
	}

	private List<String> getDefaultStringList(String key, List<String> def) {
		if (defaultSection == null || !defaultSection.contains(key, true)) return def;
		return defaultSection.getStringList(key);
	}

	private ConfigurationSection getDefaultConfigSection(String key, ConfigurationSection def) {
		if (defaultSection == null || !defaultSection.contains(key, true)) return def;
		return defaultSection.getConfigurationSection(key);
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity) {
		return hardCast(new SpellData(livingEntity));
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity, String[] args) {
		return hardCast(new SpellData(livingEntity, 1f, args));
	}

	@Deprecated
	public final SpellCastResult cast(LivingEntity livingEntity, float power, String[] args) {
		return hardCast(new SpellData(livingEntity, power, args));
	}

	// DEBUG INFO: level 2, spell cast state
	// DEBUG INFO: level 2, spell canceled
	// DEBUG INFO: level 2, spell cast state changed
	@Deprecated
	protected SpellCastEvent preCast(LivingEntity livingEntity, float power, String[] args) {
		return preCast(new SpellData(livingEntity, power, args));
	}

	// DEBUG INFO: level 3, power #
	// DEBUG INFO: level 3, cooldown #
	// DEBUG INFO: level 3, args argsvalue
	@Deprecated
	PostCastAction handleCast(SpellCastEvent spellCast) {
		return onCast(spellCast).action();
	}

	// DEBUG INFO: level 3, post cast action actionName
	@Deprecated
	protected void postCast(SpellCastEvent spellCast, PostCastAction action) {
		postCast(spellCast, new CastResult(action, spellCast.getSpellData()));
	}

	/**
	 * This method is called when a player casts a spell, either by command, with a wand item, or otherwise.
	 *
	 * @param caster the living entity casting the spell
	 * @param state  the state of the spell cast (normal, on cooldown, missing reagents, etc)
	 * @param power  the power multiplier the spell should be cast with (1.0 is normal)
	 * @param args   the spell arguments, if cast by command
	 * @return the action to take after the spell is processed
	 */
	@Deprecated
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		return cast(state, new SpellData(caster, power, args)).action();
	}

	protected SpellCastState getCastState(LivingEntity caster) {
		if (caster instanceof Player player && !MagicSpells.getSpellbook(player).canCast(this))
			return SpellCastState.CANT_CAST;
		if (worldRestrictions != null && !worldRestrictions.contains(caster.getWorld().getName()))
			return SpellCastState.WRONG_WORLD;
		if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(caster, this))
			return SpellCastState.NO_MAGIC_ZONE;
		if (onCooldown(caster)) return SpellCastState.ON_COOLDOWN;
		if (!hasReagents(caster)) return SpellCastState.MISSING_REAGENTS;
		return SpellCastState.NORMAL;
	}

	public SpellCastResult hardCast(SpellData data) {
		data = data.noTargeting();
		System.out.println("hardCast: " + Arrays.toString(data.args()));
		SpellCastEvent castEvent = preCast(data);
		if (castEvent == null) return new SpellCastResult(SpellCastState.CANT_CAST, PostCastAction.HANDLE_NORMALLY, data);

		SpellCastState state = castEvent.getSpellCastState();
		int castTime = castEvent.getCastTime();
		data = castEvent.getSpellData();

		if (castTime <= 0 || state != SpellCastState.NORMAL) {
			CastResult result = onCast(castEvent);
			return new SpellCastResult(state, result.action(), result.data());
		}

		if (!preCastTimeCheck(data.caster(), data.args()))
			return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.ALREADY_HANDLED, data);

		if (MagicSpells.useExpBarAsCastTimeBar()) new DelayedSpellCastWithBar(castEvent);
		else new DelayedSpellCast(castEvent);

		sendMessage(strCastStart, data.caster(), data);
		playSpellEffects(EffectPosition.START_CAST, data.caster(), data);

		return new SpellCastResult(SpellCastState.NORMAL, PostCastAction.DELAYED, data);
	}

	public SpellCastEvent preCast(SpellData data) {
		if (!data.hasCaster()) return null;

		SpellCastState state = getCastState(data.caster());
		debug(2, "    Spell cast state: " + state);

		SpellCastEvent castEvent = new SpellCastEvent(this, state, data, cooldown, reagents.clone(), castTime.get(data));
		if (!castEvent.callEvent()) {
			debug(2, "    Spell cancelled");
			return null;
		}

		if (castEvent.haveReagentsChanged()) {
			boolean hasReagents = hasReagents(data.caster(), castEvent.getReagents());
			if (!hasReagents && state != SpellCastState.MISSING_REAGENTS) {
				castEvent.setSpellCastState(SpellCastState.MISSING_REAGENTS);
				debug(2, "    Spell cast state changed: " + state);
			} else if (hasReagents && state == SpellCastState.MISSING_REAGENTS) {
				castEvent.setSpellCastState(state = SpellCastState.NORMAL);
				debug(2, "    Spell cast state changed: " + state);
			}
		}

		if (castEvent.hasSpellCastStateChanged()) debug(2, "    Spell cast state changed: " + state);
		if (Perm.NO_CAST_TIME.has(data.caster())) castEvent.setCastTime(0);

		return castEvent;
	}

	public CastResult onCast(SpellCastEvent castEvent) {
		SpellData data = castEvent.getSpellData();
		System.out.println("onCast: " + Arrays.toString(data.args()));
		long start = System.nanoTime();

		debug(3, "    Power: " + data.power());
		debug(3, "    Cooldown: " + castEvent.getCooldown());
		if (MagicSpells.isDebug() && data.hasArgs()) debug(3, "    Args: {" + Util.arrayJoin(data.args(), ',') + '}');

		CastResult result = cast(castEvent.getSpellCastState(), data);
		if (MagicSpells.hasProfilingEnabled()) {
			long total = MagicSpells.getProfilingTotalTime().getOrDefault(profilingKey, 0L);
			total += System.nanoTime() - start;
			MagicSpells.getProfilingTotalTime().put(profilingKey, total);

			int runs = MagicSpells.getProfilingRuns().getOrDefault(profilingKey, 0) + 1;
			MagicSpells.getProfilingRuns().put(profilingKey, runs);
		}

		postCast(castEvent, result);
		return result;
	}

	public void postCast(SpellCastEvent castEvent, CastResult result) {
		debug(3, "    Post-cast action: " + result.action());

		SpellData data = result.data();
		LivingEntity caster = data.caster();

		switch (castEvent.getSpellCastState()) {
			case NORMAL -> {
				PostCastAction action = result.action();
				if (action.setCooldown()) setCooldown(caster, castEvent.getCooldown());
				if (action.chargeReagents()) removeReagents(caster, castEvent.getReagents());
				if (action.sendMessages()) sendMessages(data);

				int experience = this.experience.get(data);
				if (experience > 0 && caster instanceof Player player) player.giveExp(experience);
			}
			case ON_COOLDOWN -> {
				MagicSpells.sendMessage(strOnCooldown, caster, data,
					"%c", String.valueOf(Math.round(getCooldown(caster))),
					"%s", getName());
				playSpellEffects(EffectPosition.COOLDOWN, caster, data);

				if (soundOnCooldown != null && caster instanceof Player player)
					player.playSound(caster.getLocation(), soundOnCooldown, 1F, 1F);
			}
			case MISSING_REAGENTS -> {
				MagicSpells.sendMessage(strMissingReagents, caster, data);
				playSpellEffects(EffectPosition.MISSING_REAGENTS, caster, data);

				if (MagicSpells.showStrCostOnMissingReagents() && strCost != null && !strCost.isEmpty())
					MagicSpells.sendMessage("    (" + strCost + ')', caster, data);

				if (soundMissingReagents != null && caster instanceof Player player)
					player.playSound(caster.getLocation(), soundMissingReagents, 1F, 1F);
			}
			case CANT_CAST -> MagicSpells.sendMessage(strCantCast, caster, data);
			case NO_MAGIC_ZONE -> MagicSpells.getNoMagicZoneManager().sendNoMagicMessage(this, data);
			case WRONG_WORLD -> MagicSpells.sendMessage(strWrongWorld, caster, data);
		}

		new SpellCastedEvent(castEvent, result).callEvent();
	}

	public CastResult cast(SpellCastState state, SpellData data) {
		return state == SpellCastState.NORMAL ? cast(data) : new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public CastResult cast(SpellData data) {
		return new CastResult(castSpell(data.caster(), SpellCastState.NORMAL, data.power(), data.args()), data);
	}

	@Deprecated
	public void sendMessages(LivingEntity caster, String[] args) {
		sendMessages(new SpellData(caster, 1f, args));
	}

	public void sendMessages(SpellData data, String... replacements) {
		replacements = getReplacements(data, replacements);

		sendMessage(strCastSelf, data.caster(), data, replacements);
		sendMessage(strCastTarget, data.target(), data, replacements);
		sendMessageNear(strCastOthers, data, broadcastRange.get(data), replacements);
	}

	protected boolean preCastTimeCheck(LivingEntity livingEntity, String[] args) {
		return true;
	}

	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}

	protected List<String> tabCompletePlayerName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<>();
		partial = partial.toLowerCase();
		// TODO stream this
		for (Player p : Bukkit.getOnlinePlayers()) {
			String name = p.getName();
			if (!name.toLowerCase().startsWith(partial)) continue;
			if (sender.isOp() || !(sender instanceof Player player) || player.canSee(p)) matches.add(name);
		}
		if (!matches.isEmpty()) return matches;
		return null;
	}

	protected List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		return TxtUtil.tabCompleteSpellName(sender, partial);
	}

	// TODO can this safely be made varargs?
	/**
	 * This method is called when the spell is cast from the console.
	 *
	 * @param sender the console sender.
	 * @param args   the command arguments
	 * @return true if the spell was handled, false otherwise
	 */
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

	public abstract boolean canCastWithItem();

	public abstract boolean canCastByCommand();

	public boolean canCastWithLeftClick() {
		return castWithLeftClick;
	}

	public boolean canCastWithRightClick() {
		return castWithRightClick;
	}

	public boolean hasPreciseCooldowns() {
		return usePreciseCooldowns;
	}

	public boolean isAlwaysGranted() {
		return alwaysGranted;
	}

	public boolean isIgnoringGlobalCooldown() {
		return ignoreGlobalCooldown;
	}

	public boolean isValidItemForCastCommand(ItemStack item) {
		if (!requireCastItemOnCommand || castItems == null) return true;
		if (item == null && castItems.length == 1 && BlockUtils.isAir(castItems[0].getType())) return true;
		for (CastItem castItem : castItems) {
			if (castItem.equals(new CastItem(item))) return true;
		}
		return false;
	}

	public boolean canBind(CastItem item) {
		if (!bindable) return false;
		if (bindableItems == null) return true;
		return bindableItems.contains(item);
	}

	public ItemStack getSpellIcon() {
		return spellIcon;
	}

	public String getCostStr() {
		if (strCost == null || strCost.isEmpty()) return null;
		return strCost;
	}

	/**
	 * Check whether this spell is currently on cooldown for the specified player
	 *
	 * @param livingEntity The living entity to check
	 * @return whether the spell is on cooldown
	 */
	public boolean onCooldown(LivingEntity livingEntity) {
		if (Perm.NO_COOLDOWN.has(livingEntity)) return false;
		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) return true;

		Long next = nextCast.get(livingEntity.getUniqueId());
		return next != null && next > System.currentTimeMillis();
	}

	public float getCooldown() {
		return cooldown;
	}

	/**
	 * Get how many seconds remain on the cooldown of this spell for the specified player
	 *
	 * @param livingEntity The living entity to check
	 * @return The number of seconds remaining in the cooldown
	 */
	public float getCooldown(LivingEntity livingEntity) {
		float cd = 0;

		Long next = nextCast.get(livingEntity.getUniqueId());
		if (next != null) {
			float c = (next - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			cd = c > 0 ? c : 0;
		}

		if (serverCooldown > 0 && nextCastServer > System.currentTimeMillis()) {
			float c = (nextCastServer - System.currentTimeMillis()) / ((float) TimeUtil.MILLISECONDS_PER_SECOND);
			if (c > cd) cd = c;
		}

		return cd;
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(LivingEntity livingEntity, float cooldown) {
		setCooldown(livingEntity, cooldown, true);
	}

	/**
	 * Begins the cooldown for the spell for the specified player
	 *
	 * @param livingEntity The living entity to set the cooldown for
	 */
	public void setCooldown(final LivingEntity livingEntity, float cooldown, boolean activateSharedCooldowns) {
		final UUID uuid = livingEntity.getUniqueId();

		if (cooldown > 0 || minCooldown > 0) {
			float cd = cooldown;
			// calculate random cooldown
			if (minCooldown != -1F) {
				if (usePreciseCooldowns) cd = minCooldown + (maxCooldown - minCooldown) * random.nextFloat();
				else cd = minCooldown + random.nextInt((int) maxCooldown - (int) minCooldown + 1);
			}

			if (charges > 0) {
				chargesConsumed.increment(uuid);
				MagicSpells.scheduleDelayedTask(() -> {
					chargesConsumed.decrement(uuid);
					playSpellEffects(EffectPosition.CHARGE_USE, livingEntity, new SpellData(livingEntity));
					if (rechargeSound == null) return;
					if (rechargeSound.isEmpty()) return;
					if (livingEntity instanceof Player player)
						player.playSound(livingEntity.getLocation(), rechargeSound, 1.0F, 1.0F);
				}, Math.round(TimeUtil.TICKS_PER_SECOND * cd));
			}
			if (charges <= 0 || chargesConsumed.get(uuid) >= charges) {
				nextCast.put(uuid, System.currentTimeMillis() + (long) (cd * TimeUtil.MILLISECONDS_PER_SECOND));
			}

		} else {
			nextCast.remove(uuid);
			chargesConsumed.remove(uuid);
		}
		if (serverCooldown > 0)
			nextCastServer = System.currentTimeMillis() + (long) (serverCooldown * TimeUtil.MILLISECONDS_PER_SECOND);
		if (activateSharedCooldowns && sharedCooldowns != null) {
			for (Map.Entry<Spell, Float> scd : sharedCooldowns.entrySet()) {
				scd.getKey().setCooldown(livingEntity, scd.getValue(), false);
			}
		}
	}

	/**
	 * Checks if a player has the reagents required to cast this spell
	 *
	 * @param livingEntity the living entity to check
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity) {
		return hasReagents(livingEntity, reagents);
	}

	// FIXME this doesn't seem strictly tied to Spell logic, could probably be moved

	/**
	 * Checks if a player has the reagents required to cast this spell
	 *
	 * @param livingEntity the living entity to check
	 * @param reagents     the reagents to check for
	 * @return true if the player has the reagents, false otherwise
	 */
	protected boolean hasReagents(LivingEntity livingEntity, SpellReagents reagents) {
		if (reagents == null) return true;
		return SpellUtil.hasReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	/**
	 * Removes the reagent cost of this spell from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 *
	 * @param livingEntity the living entity to remove reagents from
	 */
	protected void removeReagents(LivingEntity livingEntity) {
		removeReagents(livingEntity, reagents);
	}

	// TODO can this safely be made varargs?
	/**
	 * Removes the specified reagents from the player's inventory.
	 * This does not check if the player has the reagents, use hasReagents() for that.
	 *
	 * @param livingEntity the living entity to remove the reagents from
	 * @param reagents     the inventory item reagents to remove
	 */
	protected void removeReagents(LivingEntity livingEntity, SpellReagents.ReagentItem[] reagents) {
		SpellUtil.removeReagents(livingEntity, reagents, 0, 0, 0, 0, 0, 0, 0, null);
	}

	protected void removeReagents(LivingEntity livingEntity, SpellReagents reagents) {
		SpellUtil.removeReagents(livingEntity, reagents.getItemsAsArray(), reagents.getHealth(), reagents.getMana(), reagents.getHunger(), reagents.getExperience(), reagents.getLevels(), reagents.getDurability(), reagents.getMoney(), reagents.getVariables());
	}

	public EnumMap<EffectPosition, List<SpellEffect>> getEffects() {
		return effects;
	}

	protected int getRange(float power) {
		return getRange(new SpellData(null, power, null));
	}

	protected int getRange(SpellData data) {
		int range = this.range.get(data);
		return spellPowerAffectsRange.get(data) ? Math.round(range * data.power()) : range;
	}

	public int getCharges() {
		return charges;
	}

	/**
	 * Get how many charges the specified living entity has consumed.
	 *
	 * @param livingEntity The living entity to check
	 * @return The number of charges consumed
	 */
	public int getCharges(LivingEntity livingEntity) {
		return chargesConsumed.get(livingEntity.getUniqueId());
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 *
	 * @param livingEntity the living entity to get the target for
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(LivingEntity livingEntity, float power) {
		return getTargetedPlayer(new SpellData(livingEntity, power, null));
	}

	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 *
	 * @param data spell data
	 * @return the targeted Player, or null if none was found
	 */
	protected TargetInfo<Player> getTargetedPlayer(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data, true, e -> e instanceof Player);
		return new TargetInfo<>((Player) info.target(), info.spellData(), info.cancelled());
	}

	protected TargetInfo<Player> getTargetPlayer(LivingEntity caster, float power) {
		return getTargetedPlayer(caster, power);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power) {
		return getTargetedEntity(new SpellData(caster, power, null), false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, ValidTargetChecker checker) {
		return getTargetedEntity(new SpellData(caster, power, null), false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(LivingEntity caster, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		return getTargetedEntity(new SpellData(caster, power, null), forceTargetPlayers, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data) {
		return getTargetedEntity(data, false, null);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data, ValidTargetChecker checker) {
		return getTargetedEntity(data, false, checker);
	}

	protected TargetInfo<LivingEntity> getTargetedEntity(SpellData data, boolean forceTargetPlayers, ValidTargetChecker checker) {
		LivingEntity caster = data.caster();
		if (targetSelf.get(data) || validTargetList.canTargetSelf()) {
			if (checker != null && !checker.isValidTarget(caster)) return new TargetInfo<>(null, data, false);

			SpellTargetEvent event = new SpellTargetEvent(this, data, caster);
			return new TargetInfo<>(event.callEvent() ? event.getTarget() : null, event.getSpellData(), event.isCastCancelled());
		}

		int currentRange = getRange(data);
		List<Entity> nearbyEntities = caster.getNearbyEntities(currentRange, currentRange, currentRange);

		// Get valid targets
		List<LivingEntity> entities;
		if (MagicSpells.checkWorldPvpFlag() && validTargetList.canTargetPlayers() && !isBeneficial() && !caster.getWorld().getPVP()) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities, false);
		} else if (forceTargetPlayers) {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities, true);
		} else {
			entities = validTargetList.filterTargetListCastingAsLivingEntities(caster, nearbyEntities);
		}

		if (checker != null) entities.removeIf(entity -> !checker.isValidTarget(entity));

		// Find target
		BlockIterator blockIterator;
		try {
			blockIterator = new BlockIterator(caster, currentRange);
		} catch (IllegalStateException e) {
			DebugHandler.debugIllegalState(e);
			return new TargetInfo<>(null, data, false);
		}

		Block block;
		Location location;

		int blockX;
		int blockY;
		int blockZ;

		double entityX;
		double entityY;
		double entityZ;

		// How far can a target be from the line of sight along the x, y, and z directions
		double xLower = 0.75;
		double xUpper = 1.75;
		double yLower = 1;
		double yUpper = 2.5;
		double zLower = 0.75;
		double zUpper = 1.75;

		// Do min range
		for (int i = 0, minRange = this.minRange.get(data); i < minRange && blockIterator.hasNext(); i++) {
			blockIterator.next();
		}

		Set<Entity> blacklistedEntities = new HashSet<>();

		// Loop through player's line of sight
		while (blockIterator.hasNext()) {
			block = blockIterator.next();
			blockX = block.getX();
			blockY = block.getY();
			blockZ = block.getZ();

			// Line of sight is broken, stop without target
			if (obeyLos && !BlockUtils.isTransparent(this, block)) break;

			// Check for entities near this block in the line of sight
			for (LivingEntity target : entities) {
				if (blacklistedEntities.contains(target)) continue;
				location = target.getLocation();
				entityX = location.getX();
				entityY = location.getY();
				entityZ = location.getZ();

				if (!(blockX - xLower <= entityX && entityX <= blockX + xUpper)) continue;
				if (!(blockY - yLower <= entityY && entityY <= blockY + yUpper)) continue;
				if (!(blockZ - zLower <= entityZ && entityZ <= blockZ + zUpper)) continue;

				// Check for invalid target
				if (target instanceof Player pl && (pl.getGameMode() == GameMode.CREATIVE || pl.getGameMode() == GameMode.SPECTATOR)) {
					blacklistedEntities.add(target);
					continue;
				}

				// Check for no-magic-zone
				if (MagicSpells.getNoMagicZoneManager() != null && MagicSpells.getNoMagicZoneManager().willFizzle(target.getLocation(), this)) {
					blacklistedEntities.add(target);
					continue;
				}

				// Check for teams
				if (target instanceof Player && MagicSpells.checkScoreboardTeams()) {
					Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

					Team playerTeam = null;
					if (caster instanceof Player) playerTeam = scoreboard.getEntryTeam(caster.getName());
					Team targetTeam = scoreboard.getEntryTeam(target.getName());

					if (playerTeam != null && targetTeam != null) {
						if (playerTeam.equals(targetTeam)) {
							if (!playerTeam.allowFriendlyFire() && !isBeneficial()) {
								blacklistedEntities.add(target);
								continue;
							}
						} else if (isBeneficial()) {
							blacklistedEntities.add(target);
							continue;
						}
					}
				}

				// Call event listeners
				SpellTargetEvent targetEvent = new SpellTargetEvent(this, data, target);
				targetEvent.callEvent();

				if (targetEvent.isCastCancelled()) return new TargetInfo<>(null, targetEvent.getSpellData(), true);
				else if (targetEvent.isCancelled()) {
					blacklistedEntities.add(target);
					continue;
				} else {
					target = targetEvent.getTarget();
				}

				// Call damage event
				if (targetDamageCause != null) {
					EntityDamageByEntityEvent entityDamageEvent = new MagicSpellsEntityDamageByEntityEvent(caster, target, targetDamageCause, targetDamageAmount, this);
					EventUtil.call(entityDamageEvent);
					if (entityDamageEvent.isCancelled()) {
						blacklistedEntities.add(target);
						continue;

					}
				}

				return new TargetInfo<>(target, targetEvent.getSpellData(), false);
			}
		}

		return new TargetInfo<>(null, data, false);
	}

	protected Block getTargetedBlock(LivingEntity entity, float power) {
		return BlockUtils.getTargetBlock(this, entity, getRange(new SpellData(entity, power, null)));
	}

	protected Block getTargetedBlock(SpellData data) {
		return BlockUtils.getTargetBlock(this, data.caster(), getRange(data));
	}

	protected List<Block> getLastTwoTargetedBlocks(LivingEntity entity, float power) {
		return BlockUtils.getLastTwoTargetBlock(this, entity, getRange(new SpellData(entity, power, null)));
	}

	protected List<Block> getLastTwoTargetedBlocks(SpellData data) {
		return BlockUtils.getLastTwoTargetBlock(this, data.caster(), getRange(data));
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data) {
		return getTargetedBlockLocation(data, 0, 0, 0, true);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, boolean allowAir) {
		return getTargetedBlockLocation(data, 0, 0, 0, allowAir);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, double offX, double offY, double offZ) {
		return getTargetedBlockLocation(data, offX, offY, offZ, true);
	}

	public TargetInfo<Location> getTargetedBlockLocation(SpellData data, double offX, double offY, double offZ, boolean allowAir) {
		Block block = getTargetedBlock(data);
		if (!allowAir && block.getType().isAir()) return new TargetInfo<>(null, data, false);

		Location location = block.getLocation();
		location.add(offX, offY, offZ);

		location.setDirection(location.toVector().subtract(data.caster().getLocation().toVector()));

		SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, data, location);
		if (!event.callEvent()) return new TargetInfo<>(null, event.getSpellData(), event.isCastCancelled());

		return new TargetInfo<>(event.getTargetLocation(), event.getSpellData(), false);
	}

	public Set<Material> getLosTransparentBlocks() {
		return losTransparentBlocks;
	}

	public boolean isTransparent(Block block) {
		return losTransparentBlocks.contains(block.getType());
	}

	protected void playSpellEffects(SpellData data) {
		LivingEntity caster = data.caster();
		LivingEntity target = data.target();
		Location location = data.location();

		if (caster != null) playSpellEffects(EffectPosition.CASTER, caster, data);

		if (target != null) playSpellEffects(EffectPosition.TARGET, target, data);
		else if (location != null) playSpellEffects(EffectPosition.TARGET, location, data);

		if (location != null && target != null) {
			playSpellEffects(EffectPosition.START_POSITION, location, data);
			playSpellEffects(EffectPosition.END_POSITION, target, data);
			playSpellEffectsTrail(location, target.getLocation(), data);

			return;
		}

		if (caster != null && target != null) {
			playSpellEffects(EffectPosition.START_POSITION, caster, data);
			playSpellEffects(EffectPosition.END_POSITION, target, data);
			playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);

			return;
		}

		if (caster != null && location != null) {
			playSpellEffects(EffectPosition.START_POSITION, caster, data);
			playSpellEffects(EffectPosition.END_POSITION, location, data);
			playSpellEffectsTrail(caster.getLocation(), location);
		}
	}

	@Deprecated
	protected void playSpellEffects(Entity caster, Entity target) {
		playSpellEffects(caster, target, SpellData.NULL);
	}

	protected void playSpellEffects(Entity caster, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, caster, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(caster.getLocation(), target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Entity caster, Location target) {
		playSpellEffects(caster, target, SpellData.NULL);
	}

	protected void playSpellEffects(Entity caster, Location target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, caster, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(caster.getLocation(), target, data);
	}

	protected void playSpellEffects(Entity caster, Location from, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, caster, data);
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, from, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(from, target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Location from, Entity target) {
		playSpellEffects(from, target, SpellData.NULL);
	}

	protected void playSpellEffects(Location from, Entity target, SpellData data) {
		playSpellEffects(EffectPosition.TARGET, target, data);
		playSpellEffects(EffectPosition.START_POSITION, from, data);
		playSpellEffects(EffectPosition.END_POSITION, target, data);
		playSpellEffectsTrail(from, target.getLocation(), data);
	}

	@Deprecated
	protected void playSpellEffects(Location startLoc, Location endLoc) {
		playSpellEffects(startLoc, endLoc, SpellData.NULL);
	}

	protected void playSpellEffects(Location startLoc, Location endLoc, SpellData data) {
		playSpellEffects(EffectPosition.CASTER, startLoc, data);
		playSpellEffects(EffectPosition.TARGET, endLoc, data);
		playSpellEffects(EffectPosition.START_POSITION, startLoc, data);
		playSpellEffects(EffectPosition.END_POSITION, endLoc, data);
		playSpellEffectsTrail(startLoc, endLoc, data);
	}

	@Deprecated
	protected void playSpellEffects(EffectPosition pos, Entity entity) {
		playSpellEffects(pos, entity, SpellData.NULL);
	}

	protected void playSpellEffects(EffectPosition pos, Entity entity, SpellData data) {
		if (effects == null) return;

		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;

		for (SpellEffect effect : effectsList) {
			Runnable canceler = effect.playEffect(entity, data);
			if (canceler == null) continue;
			if (!(entity instanceof Player player)) continue;

			Map<EffectPosition, List<Runnable>> runnablesMap = callbacks.get(player.getUniqueId().toString());
			if (runnablesMap == null) continue;

			List<Runnable> runnables = runnablesMap.get(pos);
			if (runnables == null) continue;

			runnables.add(canceler);
		}
	}

	@Deprecated
	protected void playSpellEffects(EffectPosition pos, Location location) {
		playSpellEffects(pos, location, SpellData.NULL);
	}

	protected void playSpellEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return;
		for (SpellEffect effect : effectsList) {
			effect.playEffect(location, data);
		}
	}

	@Deprecated
	protected Set<EffectlibSpellEffect> playSpellEffectLibEffects(EffectPosition pos, Location location) {
		return playSpellEffectLibEffects(pos, location, SpellData.NULL);
	}

	protected Set<EffectlibSpellEffect> playSpellEffectLibEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<EffectlibSpellEffect> spellEffects = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EffectLibEffect)) continue;
			spellEffects.add(new EffectlibSpellEffect(effect.playEffectLib(location, data), (EffectLibEffect) effect));
		}
		return spellEffects;
	}

	@Deprecated
	protected Map<SpellEffect, Entity> playSpellEntityEffects(EffectPosition pos, Location location) {
		return playSpellEntityEffects(pos, location, SpellData.NULL);
	}

	protected Map<SpellEffect, Entity> playSpellEntityEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Map<SpellEffect, Entity> values = new HashMap<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof EntityEffect)) continue;
			values.put(effect, effect.playEntityEffect(location, data));
		}
		return values;
	}

	@Deprecated
	protected Set<ArmorStand> playSpellArmorStandEffects(EffectPosition pos, Location location) {
		return playSpellArmorStandEffects(pos, location, SpellData.NULL);
	}

	protected Set<ArmorStand> playSpellArmorStandEffects(EffectPosition pos, Location location, SpellData data) {
		if (effects == null) return null;
		List<SpellEffect> effectsList = effects.get(pos);
		if (effectsList == null) return null;
		Set<ArmorStand> armorStands = new HashSet<>();
		for (SpellEffect effect : effectsList) {
			if (!(effect instanceof ArmorStandEffect)) continue;
			armorStands.add(effect.playArmorStandEffect(location, data));
		}
		return armorStands;
	}

	@Deprecated
	protected void playSpellEffectsTrail(Location loc1, Location loc2) {
		playSpellEffectsTrail(loc1, loc2, SpellData.NULL);
	}

	protected void playSpellEffectsTrail(Location loc1, Location loc2, SpellData data) {
		if (effects == null) return;
		if (!LocationUtil.isSameWorld(loc1, loc2)) return;
		List<SpellEffect> effectsList = effects.get(EffectPosition.TRAIL);
		if (effectsList != null) {
			for (SpellEffect effect : effectsList) {
				effect.playEffect(loc1, loc2, data);
			}
		}
		List<SpellEffect> rTrailEffects = effects.get(EffectPosition.REVERSE_LINE);
		if (rTrailEffects != null) {
			for (SpellEffect effect : rTrailEffects) {
				effect.playEffect(loc2, loc1, data);
			}
		}
	}

	@Deprecated
	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity) {
		playTrackingLinePatterns(pos, origin, target, originEntity, targetEntity, SpellData.NULL);
	}

	public void playTrackingLinePatterns(EffectPosition pos, Location origin, Location target, Entity originEntity, Entity targetEntity, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> spellEffects = effects.get(pos);
		if (spellEffects == null) return;
		for (SpellEffect e : spellEffects) {
			e.playTrackingLinePatterns(origin, target, originEntity, targetEntity, data);
		}
	}

	public void initializePlayerEffectTracker(Player p) {
		if (callbacks == null) return;
		String key = p.getUniqueId().toString();
		Map<EffectPosition, List<Runnable>> entry = new EnumMap<>(EffectPosition.class);
		for (EffectPosition pos : EffectPosition.values()) {
			entry.put(pos, new ArrayList<>());
		}
		callbacks.put(key, entry);
	}

	public void unloadPlayerEffectTracker(Player p) {
		String uuid = p.getUniqueId().toString();
		for (EffectPosition pos : EffectPosition.values()) {
			cancelEffects(pos, uuid);
		}
		callbacks.remove(uuid);
	}

	public void cancelEffects(EffectPosition pos, String uuid) {
		if (callbacks == null) return;
		if (callbacks.get(uuid) == null) return;
		List<Runnable> cancelers = callbacks.get(uuid).get(pos);
		while (!cancelers.isEmpty()) {
			Runnable c = cancelers.iterator().next();
			if (c instanceof Effect eff) eff.cancel();
			else c.run();
			cancelers.remove(c);
		}
	}

	public void cancelEffectForAllPlayers(EffectPosition pos) {
		for (String key : callbacks.keySet()) {
			cancelEffects(pos, key);
		}
	}

	public Set<EffectTracker> getEffectTrackers() {
		return effectTrackerSet;
	}

	public Set<AsyncEffectTracker> getAsyncEffectTrackers() {
		return asyncEffectTrackerSet;
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker, SpellData data) {
		if (effects == null) return;
		List<SpellEffect> effectList = effects.get(EffectPosition.BUFF);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOnEntity(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.ORBIT);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				EffectTracker tracker = effect.playEffectWhileActiveOrbit(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				effectTrackerSet.add(tracker);
			}
		}

		effectList = effects.get(EffectPosition.BUFF_EFFECTLIB);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				if (!(effect instanceof EffectLibEffect)) continue;
				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOnEntity(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				asyncEffectTrackerSet.add(tracker);
			}
		}

		// only normal effectlib effect is allowed
		effectList = effects.get(EffectPosition.ORBIT_EFFECTLIB);
		if (effectList != null) {
			for (SpellEffect effect : effectList) {
				if (!(effect instanceof EffectLibEffect)) continue;
				if (effect instanceof EffectLibLineEffect) continue;
				if (effect instanceof EffectLibEntityEffect) continue;

				AsyncEffectTracker tracker = effect.playEffectlibEffectWhileActiveOrbit(entity, checker, data);
				if (this instanceof BuffSpell) tracker.setBuffSpell((BuffSpell) this);
				asyncEffectTrackerSet.add(tracker);
			}
		}
	}

	protected void playSpellEffectsBuff(Entity entity, SpellEffect.SpellEffectActiveChecker checker) {
		playSpellEffectsBuff(entity, checker, null);
	}

	protected void registerEvents() {
		registerEvents(this);
	}

	protected void registerEvents(Listener listener) {
		MagicSpells.registerEvents(listener);
	}

	protected void unregisterEvents(Listener listener) {
		HandlerList.unregisterAll(listener);
	}

	protected int scheduleDelayedTask(Runnable task, int delay) {
		return MagicSpells.scheduleDelayedTask(task, delay);
	}

	protected int scheduleRepeatingTask(Runnable task, int delay, int interval) {
		return MagicSpells.scheduleRepeatingTask(task, delay, interval);
	}

	protected CastItem[] setupCastItems(String[] items, String errorMessage) {
		CastItem[] castItems = new CastItem[items.length];
		for (int i = 0; i < items.length; i++) {
			MagicItem magicItem = MagicItems.getMagicItemFromString(items[i]);
			if (magicItem == null) {
				MagicSpells.error(errorMessage.replace("%i", items[i]));
				continue;
			}

			ItemStack item = magicItem.getItemStack();
			if (item == null) {
				MagicSpells.error(errorMessage.replace("%i", items[i]));
				continue;
			}
			castItems[i] = new CastItem(item);
		}
		return castItems;
	}

	protected Subspell initSubspell(String subspellName, String errorMessage) {
		if (subspellName.isEmpty()) return null;

		Subspell subspell = new Subspell(subspellName);
		if (!subspell.process()) {
			MagicSpells.error(errorMessage);
			return null;
		}

		return subspell;
	}

	/**
	 * Formats a string by performing the specified replacements.
	 *
	 * @param message      the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	protected String formatMessage(String message, String... replacements) {
		return MagicSpells.formatMessage(message, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 *
	 * @param livingEntity the living entity to send the message to
	 * @param message      the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(LivingEntity livingEntity, String message, String... replacements) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL, replacements);
	}

	protected void sendMessage(LivingEntity livingEntity, String message) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL);
	}

	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 *
	 * @param livingEntity the living entity to send the message to
	 * @param message      the message to send
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL.args(args));
	}

	protected void sendMessage(String message, LivingEntity livingEntity) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param livingEntity the player to send the message to
	 * @param args         the arguments of associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, LivingEntity livingEntity, String[] args, String... replacements) {
		MagicSpells.sendMessage(message, livingEntity, SpellData.NULL.args(args), replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param data         the data of the associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, SpellData data, String... replacements) {
		MagicSpells.sendMessage(message, data.caster(), data, replacements);
	}

	/**
	 * Sends a message to a player, first making the specified replacements.This method also does color replacement and has multi-line functionality.
	 *
	 * @param message      the message to send
	 * @param recipient    the player to send the message to
	 * @param data         the data of the associated spell cast
	 * @param replacements the replacements to be made, in pairs
	 */
	protected void sendMessage(String message, LivingEntity recipient, SpellData data, String... replacements) {
		MagicSpells.sendMessage(message, recipient, data, replacements);
	}

	/**
	 * Sends a message to all players near the specified player, within the configured broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 */
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, String message) {
		SpellData data = new SpellData(livingEntity);
		sendMessageNear(message, data, broadcastRange.get(data));
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param message the message to send
	 * @param range the broadcast range
	 */
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args) {
		sendMessageNear(message, new SpellData(livingEntity, ignore, 1f, args), range);
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 * @param livingEntity the "center" living entity used to find nearby players
	 * @param ignore player to ignore when sending messages
	 * @param message the message to send
	 * @param range the broadcast range
	 * @param args cast arguments
	 * @param replacements replacements to be done on message
	 */
	@Deprecated
	protected void sendMessageNear(LivingEntity livingEntity, Player ignore, String message, int range, String[] args, String... replacements) {
		sendMessageNear(message, new SpellData(livingEntity, ignore, 1f, args), range, replacements);
	}

	/**
	 * Sends a message to all players near the specified player, within the specified broadcast range.
	 *
	 * @param message      the message to send
	 * @param data         the associated spell data
	 * @param range        the broadcast range
	 * @param replacements replacements to be done on message
	 */
	protected void sendMessageNear(String message, SpellData data, int range, String... replacements) {
		if (message == null || message.isEmpty() || Perm.SILENT.has(data.caster())) return;

		Collection<Player> players = data.caster().getLocation().getNearbyPlayers(range);
		for (Player player : players) {
			if (Objects.equals(player, data.caster()) || Objects.equals(player, data.target())) continue;
			MagicSpells.sendMessage(message, player, data, replacements);
		}
	}

	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(SpellData data) {
		if (!playFizzleSound.get(data) || !(data.caster() instanceof Player player)) return;
		player.playEffect(player.getLocation(), org.bukkit.Effect.EXTINGUISH, null);
	}

	protected String getTargetName(LivingEntity target) {
		return MagicSpells.getTargetName(target);
	}

	protected String[] getReplacements(SpellData data, String... replacements) {
		List<String> replacementList = new ArrayList<>(Arrays.asList(replacements));

		if (data.hasCaster()) {
			replacementList.add("%a");
			replacementList.add(getTargetName(data.caster()));
		}

		if (data.hasTarget()) {
			replacementList.add("%t");
			replacementList.add(getTargetName(data.target()));
		}

		return replacementList.toArray(new String[0]);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param event targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellTargetEvent event) {
		return noTarget(strNoTarget, event);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param event targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellTargetLocationEvent event) {
		return noTarget(strNoTarget, event);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param info targeting info of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(TargetInfo<?> info) {
		return noTarget(strNoTarget, info);
	}

	/**
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 *
	 * @param data spell data of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(SpellData data) {
		return noTarget(strNoTarget, data);
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param event   targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellTargetEvent event) {
		if (event.isCastCancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, event.getSpellData());
		return noTarget(message, event.getSpellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param event   targeting event that was cancelled
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellTargetLocationEvent event) {
		if (event.isCastCancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, event.getSpellData());
		return noTarget(message, event.getSpellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param info    targeting info of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, TargetInfo<?> info) {
		if (info.cancelled()) return new CastResult(PostCastAction.ALREADY_HANDLED, info.spellData());
		return noTarget(message, info.spellData());
	}

	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 *
	 * @param message the message to send
	 * @param data    spell data of the spell cast
	 * @return the appropriate PostCastAction value
	 */
	protected CastResult noTarget(String message, SpellData data) {
		fizzle(data);
		if (message != null && !message.isEmpty()) sendMessage(message, data, getReplacements(data));
		if (spellOnFail != null) spellOnFail.subcast(data.noTargeting());
		return new CastResult(alwaysActivate.get(data) ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED, data);
	}

	public String getInternalName() {
		return internalName;
	}

	public String getName() {
		if (name != null && !name.isEmpty()) return name;
		return internalName;
	}

	public String getPermissionName() {
		return permName;
	}

	public boolean isHelperSpell() {
		return helperSpell;
	}

	public String getCantBindError() {
		return strCantBind;
	}

	public String[] getAliases() {
		return aliases;
	}

	public List<String> getIncantations() {
		return incantations;
	}

	public CastItem getCastItem() {
		if (castItems.length == 1) return castItems[0];
		return null;
	}

	public CastItem[] getCastItems() {
		return castItems;
	}

	public CastItem[] getLeftClickCastItems() {
		return leftClickCastItems;
	}

	public CastItem[] getRightClickCastItems() {
		return rightClickCastItems;
	}

	public CastItem[] getConsumeCastItems() {
		return consumeCastItems;
	}

	public String getDanceCastSequence() {
		return danceCastSequence;
	}

	public String getDescription() {
		return description;
	}

	public SpellReagents getReagents() {
		return reagents;
	}

	public String getStrWrongCastItem() {
		return strWrongCastItem;
	}

	public List<String> getPrecludes() {
		return precludes;
	}

	public List<String> getReplaces() {
		return replaces;
	}

	public List<String> getPrerequisites() {
		return prerequisites;
	}

	public final boolean isBeneficial() {
		return beneficial;
	}

	public boolean isBeneficialDefault() {
		return false;
	}

	public ModifierSet getModifiers() {
		return modifiers;
	}

	public ModifierSet getTargetModifiers() {
		return targetModifiers;
	}

	public ModifierSet getLocationModifiers() {
		return locationModifiers;
	}

	public String getStrModifierFailed() {
		return strModifierFailed;
	}

	public Map<String, Integer> getXpGranted() {
		return xpGranted;
	}

	public Map<String, Integer> getXpRequired() {
		return xpRequired;
	}

	public String getStrXpLearned() {
		return strXpAutoLearned;
	}

	public String getStrOnTeach() {
		return strOnTeach;
	}

	public Map<UUID, Long> getCooldowns() {
		return nextCast;
	}

	public Multimap<String, VariableMod> getVariableModsCast() {
		return variableModsCast;
	}

	public Multimap<String, VariableMod> getVariableModsCasted() {
		return variableModsCasted;
	}

	public Multimap<String, VariableMod> getVariableModsTarget() {
		return variableModsTarget;
	}

	public ValidTargetList getValidTargetList() {
		return validTargetList;
	}

	public void setValidTargetList(ValidTargetList validTargetList) {
		this.validTargetList = validTargetList;
	}

	void setCooldownManually(UUID uuid, long nextCast) {
		this.nextCast.put(uuid, nextCast);
	}

	protected void debug(int level, String message) {
		if (debug) MagicSpells.debug(level, message);
	}

	/**
	 * This method is called when the plugin is being disabled, for any reason.
	 */
	protected void turnOff() {
		// No op
	}

	@Override
	public int compareTo(Spell spell) {
		return name.compareTo(spell.name);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Spell && ((Spell) o).internalName.equals(internalName);
	}

	@Override
	public int hashCode() {
		return internalName.hashCode();
	}

	// TODO move this to its own class
	public enum SpellCastState {

		NORMAL,
		ON_COOLDOWN,
		MISSING_REAGENTS,
		CANT_CAST,
		NO_MAGIC_ZONE,
		WRONG_WORLD

	}

	// TODO move this to its own class
	public enum PostCastAction {

		HANDLE_NORMALLY(true, true, true),
		ALREADY_HANDLED(false, false, false),
		NO_MESSAGES(true, true, false),
		NO_REAGENTS(true, false, true),
		NO_COOLDOWN(false, true, true),
		MESSAGES_ONLY(false, false, true),
		REAGENTS_ONLY(false, true, false),
		COOLDOWN_ONLY(true, false, false),
		DELAYED(false, false, false);

		private final boolean cooldown;
		private final boolean reagents;
		private final boolean messages;

		PostCastAction(boolean cooldown, boolean reagents, boolean messages) {
			this.cooldown = cooldown;
			this.reagents = reagents;
			this.messages = messages;
		}

		public boolean setCooldown() {
			return cooldown;
		}

		public boolean chargeReagents() {
			return reagents;
		}

		public boolean sendMessages() {
			return messages;
		}

	}

	public static class SpellCastResult {

		public SpellCastState state;
		public PostCastAction action;
		public SpellData data;

		public SpellCastResult(SpellCastState state, PostCastAction action) {
			this.state = state;
			this.action = action;
			this.data = SpellData.NULL;
		}

		public SpellCastResult(SpellCastState state, PostCastAction action, SpellData data) {
			this.state = state;
			this.action = action;
			this.data = data;
		}

		public boolean success() {
			return state == SpellCastState.NORMAL && action != PostCastAction.ALREADY_HANDLED;
		}

		public boolean fail() {
			return state != SpellCastState.NORMAL || action == PostCastAction.ALREADY_HANDLED;
		}

	}

	public class DelayedSpellCast implements Runnable, Listener {

		private static final double motionTolerance = 0.2;

		private final SpellCastEvent spellCast;
		private final LivingEntity caster;
		private final Location from;
		private final int taskId;

		private final boolean interruptOnCast;
		private final boolean interruptOnMove;
		private final boolean interruptOnDamage;
		private final boolean interruptOnTeleport;

		public DelayedSpellCast(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			taskId = scheduleDelayedTask(this, spellCast.getCastTime());
			caster = spellCast.getCaster();
			from = caster.getLocation();

			SpellData data = spellCast.getSpellData();
			interruptOnCast = Spell.this.interruptOnCast.get(data);
			interruptOnMove = Spell.this.interruptOnMove.get(data);
			interruptOnDamage = Spell.this.interruptOnDamage.get(data);
			interruptOnTeleport = Spell.this.interruptOnTeleport.get(data);

			registerEvents(this);
		}

		@Override
		public void run() {
			if (!caster.isValid() || caster.isDead()) {
				unregisterEvents(this);
				return;
			}

			if (interruptOnMove && !inBounds(caster.getLocation())) {
				interrupt();
				return;
			}

			unregisterEvents(this);

			spellCast.setSpellCastState(getCastState(caster));
			spellCast.getSpell().onCast(spellCast);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!event.getCaster().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (!event.getPlayer().equals(caster)) return;

			interrupt();
		}

		private boolean inBounds(Location to) {
			return Math.abs(from.getX() - to.getX()) < motionTolerance
				&& Math.abs(from.getY() - to.getY()) < motionTolerance
				&& Math.abs(from.getZ() - to.getZ()) < motionTolerance;
		}

		private void interrupt() {
			MagicSpells.cancelTask(taskId);
			unregisterEvents(this);

			sendMessage(strInterrupted, caster, spellCast.getSpellData());
			if (spellOnInterrupt != null)
				spellOnInterrupt.subcast(new SpellData(caster, caster.getLocation(), spellCast.getPower(), spellCast.getSpellArgs()));
		}

	}

	public class DelayedSpellCastWithBar implements Runnable, Listener {

		private static final double motionTolerance = 0.2;
		private static final int interval = 5;

		private final SpellCastEvent spellCast;
		private final LivingEntity caster;
		private final Location from;
		private final int castTime;
		private final int taskId;

		private final boolean interruptOnCast;
		private final boolean interruptOnMove;
		private final boolean interruptOnDamage;
		private final boolean interruptOnTeleport;

		private int elapsed = 0;

		public DelayedSpellCastWithBar(SpellCastEvent spellCast) {
			this.spellCast = spellCast;

			castTime = spellCast.getCastTime();
			caster = spellCast.getCaster();
			from = caster.getLocation();

			SpellData data = spellCast.getSpellData();
			interruptOnCast = Spell.this.interruptOnCast.get(data);
			interruptOnMove = Spell.this.interruptOnMove.get(data);
			interruptOnDamage = Spell.this.interruptOnDamage.get(data);
			interruptOnTeleport = Spell.this.interruptOnTeleport.get(data);

			if (caster instanceof Player pl) MagicSpells.getExpBarManager().lock(pl, this);
			taskId = scheduleRepeatingTask(this, interval, interval);
			registerEvents(this);
		}

		@Override
		public void run() {
			if (!caster.isValid() || caster.isDead()) {
				end();
				return;
			}

			elapsed += interval;

			if (interruptOnMove && !inBounds(caster.getLocation())) {
				interrupt();
				return;
			}

			if (elapsed >= castTime) {
				end();

				spellCast.setSpellCastState(getCastState(caster));
				spellCast.getSpell().onCast(spellCast);
			}

			if (caster instanceof Player pl)
				MagicSpells.getExpBarManager().update(pl, 0, (float) elapsed / (float) castTime, this);
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onMove(PlayerMoveEvent event) {
			if (!interruptOnMove) return;
			if (!event.getPlayer().equals(caster)) return;
			if (inBounds(event.getTo())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDamage(EntityDamageEvent event) {
			if (!interruptOnDamage) return;
			if (!event.getEntity().equals(caster)) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onSpellCast(SpellCastEvent event) {
			if (!interruptOnCast) return;
			if (event.getSpell() instanceof PassiveSpell) return;
			if (!caster.equals(event.getCaster())) return;

			interrupt();
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onTeleport(PlayerTeleportEvent event) {
			if (!interruptOnTeleport) return;
			if (!event.getPlayer().equals(caster)) return;

			interrupt();
		}

		private boolean inBounds(Location to) {
			return Math.abs(from.getX() - to.getX()) < motionTolerance
				&& Math.abs(from.getY() - to.getY()) < motionTolerance
				&& Math.abs(from.getZ() - to.getZ()) < motionTolerance;
		}

		private void interrupt() {
			SpellData subData = spellCast.getSpellData().builder().target(null).location(caster.getLocation()).build();
			sendMessage(strInterrupted, caster, subData);
			if (spellOnInterrupt != null) spellOnInterrupt.subcast(subData);
			end();
		}

		private void end() {
			MagicSpells.cancelTask(taskId);
			unregisterEvents(this);

			if (caster instanceof Player pl) {
				MagicSpells.getExpBarManager().unlock(pl, this);
				MagicSpells.getExpBarManager().update(pl, pl.getLevel(), pl.getExp());
				ManaHandler mana = MagicSpells.getManaHandler();
				if (mana != null) mana.showMana(pl);
			}
		}

	}

	public ValidTargetChecker getValidTargetChecker() {
		return null;
	}

}
