package com.nisovin.magicspells.spells.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.util.CastItem;
import com.nisovin.magicspells.util.MagicConfig;

public class UnbindSpell extends CommandSpell {
	
	private String strUsage;
	private String strNoSpell;
	private String strCantBindSpell;
	private String strNotBound;

	public UnbindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strUsage = getConfigString("str-usage", "You must specify a spell name.");
		strNoSpell = getConfigString("str-no-spell", "You do not know a spell by that name.");
		strCantBindSpell = getConfigString("str-cant-bind-spell", "That spell cannot be bound to an item.");
		strNotBound = getConfigString("str-not-bound", "That spell is not bound to that item.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length != 1) {
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				Spell spell = MagicSpells.getSpellByInGameName(args[0]);
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				if (spell == null || spellbook == null) {
					// fail - no such spell, or no spellbook
					sendMessage(player, strNoSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spellbook.hasSpell(spell)) {
					// fail - doesn't know spell
					sendMessage(player, strNoSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else if (!spell.canCastWithItem()) {
					// fail - spell can't be bound
					sendMessage(player, strCantBindSpell);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					CastItem item = new CastItem(player.getItemInHand());
					boolean removed = spellbook.removeCastItem(spell, item);
					if (!removed) {
						sendMessage(player, strNotBound);
						return PostCastAction.ALREADY_HANDLED;
					}
					spellbook.save();
					sendMessage(player, formatMessage(strCastSelf, "%s", spell.getName()));
					return PostCastAction.NO_MESSAGES;
				}
			}
		}		
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}

}
