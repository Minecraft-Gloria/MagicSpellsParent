package com.nisovin.magicspells.spells.command;

import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.compat.EventUtil;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;

// TODO this should not be hardcoded to use a book
public class TomeSpell extends CommandSpell {

	private static final String key = "tome_data";
	private static final Pattern INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	private boolean consumeBook;
	private boolean cancelReadOnLearn;
	private ConfigData<Boolean> allowOverwrite;
	private ConfigData<Boolean> requireTeachPerm;

	private ConfigData<Integer> maxUses;
	private ConfigData<Integer> defaultUses;

	private String strUsage;
	private String strNoBook;
	private String strNoSpell;
	private String strLearned;
	private String strCantLearn;
	private String strCantTeach;
	private String strAlreadyKnown;
	private String strAlreadyHasSpell;

	public TomeSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		consumeBook = getConfigBoolean("consume-book", false);
		allowOverwrite = getConfigDataBoolean("allow-overwrite", false);
		requireTeachPerm = getConfigDataBoolean("require-teach-perm", true);
		cancelReadOnLearn = getConfigBoolean("cancel-read-on-learn", true);

		maxUses = getConfigDataInt("max-uses", 5);
		defaultUses = getConfigDataInt("default-uses", -1);

		strUsage = getConfigString("str-usage", "Usage: While holding a written book, /cast " + name + " <spell> [uses]");
		strNoBook = getConfigString("str-no-book", "You must be holding a written book.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell with that name.");
		strLearned = getConfigString("str-learned", "You have learned the %s spell.");
		strCantLearn = getConfigString("str-cant-learn", "You cannot learn the spell in this tome.");
		strCantTeach = getConfigString("str-cant-teach", "You cannot create a tome with that spell.");
		strAlreadyKnown = getConfigString("str-already-known", "You already know the %s spell.");
		strAlreadyHasSpell = getConfigString("str-already-has-spell", "That book already contains a spell.");
	}

	@Override
	public CastResult cast(SpellData data) {
		if (!(data.caster() instanceof Player player)) return new CastResult(PostCastAction.ALREADY_HANDLED, data);

		if (!data.hasArgs()) {
			sendMessage(strUsage, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		Spellbook spellbook = MagicSpells.getSpellbook(player);
		Spell spell = MagicSpells.getSpellByInGameName(data.args()[0]);
		if (spell == null || !spellbook.hasSpell(spell)) {
			sendMessage(strNoSpell, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}
		if (requireTeachPerm.get(data) && !MagicSpells.getSpellbook(player).canTeach(spell)) {
			sendMessage(strCantTeach, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() != Material.WRITTEN_BOOK) {
			sendMessage(strNoBook, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		if (!allowOverwrite.get(data) && DataUtil.getString(item, key) != null) {
			sendMessage(strAlreadyHasSpell, player, data);
			return new CastResult(PostCastAction.ALREADY_HANDLED, data);
		}

		int uses = -1;
		if (data.args().length > 1 && RegexUtil.matches(INT_PATTERN, data.args()[1])) uses = Integer.parseInt(data.args()[1]);
		item = createTome(spell, uses, item, data);
		player.getInventory().setItemInMainHand(item);

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		return null;
	}

	public ItemStack createTome(Spell spell, int uses, ItemStack item, SpellData data) {
		int maxUses = this.maxUses.get(data);
		if (maxUses > 0 && uses > maxUses) uses = maxUses;
		else if (uses < 0) uses = defaultUses.get(data);

		if (item == null) {
			item = new ItemStack(Material.WRITTEN_BOOK, 1);
			BookMeta bookMeta = (BookMeta)item.getItemMeta();
			bookMeta.setTitle(getName() + ": " + spell.getName());
			item.setItemMeta(bookMeta);
		}

		DataUtil.setString(item, key, spell.getInternalName() + (uses > 0 ? "," + uses : ""));
		return item;
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		if (!event.hasItem()) return;
		ItemStack item = event.getItem();
		if (item == null) return;
		if (item.getType() != Material.WRITTEN_BOOK) return;
		
		String spellData = DataUtil.getString(item, key);
		if (spellData == null || spellData.isEmpty()) return;
		
		String[] data = spellData.split(",");
		Spell spell = MagicSpells.getSpellByInternalName(data[0]);
		int uses = -1;
		if (data.length > 1) uses = Integer.parseInt(data[1]);
		Spellbook spellbook = MagicSpells.getSpellbook(event.getPlayer());
		if (spell == null) return;

		if (spellbook.hasSpell(spell)) {
			sendMessage(strAlreadyKnown, event.getPlayer(), MagicSpells.NULL_ARGS, "%s", spell.getName());
			return;
		}
		if (!spellbook.canLearn(spell)) {
			sendMessage(strCantLearn, event.getPlayer(), MagicSpells.NULL_ARGS, "%s", spell.getName());
			return;
		}
		SpellLearnEvent learnEvent = new SpellLearnEvent(spell, event.getPlayer(), LearnSource.TOME, event.getPlayer().getInventory().getItemInMainHand());
		EventUtil.call(learnEvent);
		if (learnEvent.isCancelled()) {
			sendMessage(strCantLearn, event.getPlayer(), MagicSpells.NULL_ARGS, "%s", spell.getName());
			return;
		}
		spellbook.addSpell(spell);
		spellbook.save();
		sendMessage(strLearned, event.getPlayer(), MagicSpells.NULL_ARGS, "%s", spell.getName());
		if (cancelReadOnLearn) event.setCancelled(true);

		if (uses > 0) {
			uses--;
			if (uses > 0) DataUtil.setString(item, key, data[0] + "," + uses);
			else DataUtil.remove(item, key);

		}
		if (uses <= 0 && consumeBook) event.getPlayer().getInventory().setItemInMainHand(null);
		playSpellEffects(EffectPosition.DELAYED, event.getPlayer(), new SpellData(event.getPlayer()));
	}

	public static Pattern getIntPattern() {
		return INT_PATTERN;
	}

	public boolean shouldConsumeBook() {
		return consumeBook;
	}

	public void setConsumeBook(boolean consumeBook) {
		this.consumeBook = consumeBook;
	}

	public boolean shouldCancelReadOnLearn() {
		return cancelReadOnLearn;
	}

	public void setCancelReadOnLearn(boolean cancelReadOnLearn) {
		this.cancelReadOnLearn = cancelReadOnLearn;
	}

	public String getStrUsage() {
		return strUsage;
	}

	public void setStrUsage(String strUsage) {
		this.strUsage = strUsage;
	}

	public String getStrNoBook() {
		return strNoBook;
	}

	public void setStrNoBook(String strNoBook) {
		this.strNoBook = strNoBook;
	}

	public String getStrNoSpell() {
		return strNoSpell;
	}

	public void setStrNoSpell(String strNoSpell) {
		this.strNoSpell = strNoSpell;
	}

	public String getStrLearned() {
		return strLearned;
	}

	public void setStrLearned(String strLearned) {
		this.strLearned = strLearned;
	}

	public String getStrCantTeach() {
		return strCantTeach;
	}

	public void setStrCantTeach(String strCantTeach) {
		this.strCantTeach = strCantTeach;
	}

	public String getStrCantLearn() {
		return strCantLearn;
	}

	public void setStrCantLearn(String strCantLearn) {
		this.strCantLearn = strCantLearn;
	}

	public String getStrAlreadyKnown() {
		return strAlreadyKnown;
	}

	public void setStrAlreadyKnown(String strAlreadyKnown) {
		this.strAlreadyKnown = strAlreadyKnown;
	}

	public String getStrAlreadyHasSpell() {
		return strAlreadyHasSpell;
	}

	public void setStrAlreadyHasSpell(String strAlreadyHasSpell) {
		this.strAlreadyHasSpell = strAlreadyHasSpell;
	}

}
