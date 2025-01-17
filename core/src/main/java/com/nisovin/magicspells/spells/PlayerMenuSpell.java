package com.nisovin.magicspells.spells;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.util.magicitems.MagicItem;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PlayerMenuSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Component> title;

	private final String skullName;
	private final String skullNameRadius;
	private final String skullNameOffline;
	private final ConfigData<String> variableTarget;

	private final List<String> skullLore;

	private final ConfigData<Integer> delay;

	private final ConfigData<Double> radius;

	private final ConfigData<Boolean> stayOpen;
	private final ConfigData<Boolean> addOpener;
	private final ConfigData<Boolean> castSpellsOnTarget;

	private final String spellRangeName;
	private final String spellOnLeftName;
	private final String spellOfflineName;
	private final String spellOnRightName;
	private final String spellOnMiddleName;
	private final String spellOnSneakLeftName;
	private final String spellOnSneakRightName;

	private Subspell spellOffline;
	private Subspell spellRange;
	private Subspell spellOnLeft;
	private Subspell spellOnRight;
	private Subspell spellOnMiddle;
	private Subspell spellOnSneakLeft;
	private Subspell spellOnSneakRight;

	private final List<String> playerModifiersStrings;

	private ModifierSet playerModifiers;

	private final ItemStack previousPageItem;
	private final ItemStack nextPageItem;

	public PlayerMenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		title = getConfigDataComponent("title", Component.text("PlayerMenuSpell '" + internalName + "'"));

		skullName = getConfigString("skull-name", "&6%t");
		variableTarget = getConfigDataString("variable-target", null);
		skullNameRadius = getConfigString("skull-name-radius", "&4%t &3out of radius.");
		skullNameOffline = getConfigString("skull-name-offline", "&4%t");

		skullLore = getConfigStringList("skull-lore", null);

		delay = getConfigDataInt("delay", 0);

		radius = getConfigDataDouble("radius", 0);

		stayOpen = getConfigDataBoolean("stay-open", false);
		addOpener = getConfigDataBoolean("add-opener", false);
		castSpellsOnTarget = getConfigDataBoolean("cast-spells-on-target", true);

		spellRangeName = getConfigString("spell-range", "");
		spellOnLeftName = getConfigString("spell-on-left", "");
		spellOfflineName = getConfigString("spell-offline", "");
		spellOnRightName = getConfigString("spell-on-right", "");
		spellOnMiddleName = getConfigString("spell-on-middle", "");
		spellOnSneakLeftName = getConfigString("spell-on-sneak-left", "");
		spellOnSneakRightName = getConfigString("spell-on-sneak-right", "");

		playerModifiersStrings = getConfigStringList("player-modifiers", null);

		previousPageItem = createItem("previous-page-item", "Previous Page");
		nextPageItem = createItem("next-page-item", "Next Page");
	}

	@Override
	public void initializeModifiers() {
		super.initializeModifiers();

		if (playerModifiersStrings == null || playerModifiersStrings.isEmpty()) return;
		playerModifiers = new ModifierSet(playerModifiersStrings, this);
	}

	@Override
	public void initialize() {
		super.initialize();

		String error = "PlayerMenuSpell '" + internalName + "' has an invalid ";
		spellOffline = initSubspell(spellOfflineName, error + "spell-offline defined!");
		spellRange = initSubspell(spellRangeName, error + "spell-range defined!");
		spellOnLeft = initSubspell(spellOnLeftName, error + "spell-on-left defined!");
		spellOnRight = initSubspell(spellOnRightName, error + "spell-on-right defined!");
		spellOnMiddle = initSubspell(spellOnMiddleName, error + "spell-on-middle defined!");
		spellOnSneakLeft = initSubspell(spellOnSneakLeftName, error + "spell-on-sneak-left defined!");
		spellOnSneakRight = initSubspell(spellOnSneakRightName, error + "spell-on-sneak-right defined!");
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<Player> info = getTargetedPlayer(data);
		if (info.noTarget()) return noTarget(info);

		openDelay(info.target(), info.spellData());
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (!(data.target() instanceof Player target)) return noTarget(data);

		openDelay(target, data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length < 1) return false;

		Player opener = Bukkit.getPlayer(args[0]);
		if (opener == null) return false;

		String[] spellArgs = Arrays.copyOfRange(args, 1, args.length);
		openDelay(opener, new SpellData(null, opener, 1f, spellArgs));

		return true;
	}

	private ItemStack createItem(String path, String defaultName) {
		MagicItem magicItem = MagicItems.getMagicItemFromString(getConfigString(path, null));
		if (magicItem != null) return magicItem.getItemStack().clone();

		ItemStack item = new ItemStack(Material.GREEN_WOOL);
		ItemMeta meta = item.getItemMeta();

		meta.displayName(Component.text(defaultName).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);

		return item;
	}

	private void openDelay(Player opener, SpellData data) {
		int delay = this.delay.get(data);
		if (delay > 0) MagicSpells.scheduleDelayedTask(() -> open(opener, new PlayerMenuInventory(data)), delay);
		else open(opener, new PlayerMenuInventory(data));
	}

	private Component translate(Player opener, Player target, String string, SpellData data) {
		return Util.getMiniMessage(MagicSpells.doReplacements(string, opener, data,
			"%a", opener.getName(),
			"%t", target == null ? null : target.getName()
		));
	}

	private void processClickSpell(Subspell subspell, Player target, PlayerMenuInventory menu) {
		if (subspell == null) return;

		if (menu.castSpellsOnTarget) subspell.subcast(menu.openerData.target(target));
		else subspell.subcast(menu.openerData);
	}

	private void open(Player opener, PlayerMenuInventory menu) {
		List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		if (!menu.addOpener) players.remove(opener);
		if (playerModifiers != null) players.removeIf(p -> !playerModifiers.check(p));
		if (menu.radius > 0) players.removeIf(p -> opener.getLocation().distance(p.getLocation()) > menu.radius);

		int size = Math.max((int) Math.ceil(Math.min(players.size(), 54) / 9.0) * 9, 9);
		menu.createInventory(size);

		for (int i = (menu.page * 52); i < Math.min(players.size(), (menu.page + 1) * 52); i++) {
			Player player = players.get(i);
			SpellData subData = menu.openerData.target(player);

			ItemStack head = new ItemStack(Material.PLAYER_HEAD);
			head.editMeta(SkullMeta.class, meta -> {
				meta.setOwningPlayer(player);
				meta.displayName(translate(opener, player, skullName, subData));

				if (skullLore != null) {
					List<Component> lore = new ArrayList<>();
					for (String loreLine : skullLore) lore.add(translate(opener, player, loreLine, subData));

					meta.lore(lore);
				}
			});

			menu.inventory.setItem(i % 52, head);
		}

		if (menu.page > 0) menu.inventory.setItem(52, previousPageItem);
		if (players.size() > (menu.page + 1) * 52) menu.inventory.setItem(53, nextPageItem);

		opener.openInventory(menu.inventory);

		playSpellEffects(menu.data);
	}

	@EventHandler
	public void onItemClick(InventoryClickEvent event) {
		Inventory inventory = event.getClickedInventory();
		if (!(inventory.getHolder() instanceof PlayerMenuInventory menu) || menu.getSpell() != this) return;

		event.setCancelled(true);
		if (!(event.getWhoClicked() instanceof Player opener)) return;

		ItemStack item = event.getCurrentItem();
		if (item == null) return;

		ItemMeta meta = item.getItemMeta();
		if (meta == null) return;

		if (event.getRawSlot() == 52) {
			menu.page--;
			open(opener, menu);

			return;
		} else if (event.getRawSlot() == 53) {
			menu.page++;
			open(opener, menu);

			return;
		}

		if (!(meta instanceof SkullMeta skullMeta)) return;

		OfflinePlayer target = skullMeta.getOwningPlayer();
		if (target == null || !target.isOnline()) {
			meta.displayName(translate(opener, null, skullNameOffline, menu.openerData));
			if (spellOffline != null) spellOffline.subcast(menu.openerData);

			if (menu.stayOpen) item.setItemMeta(meta);
			else {
				opener.closeInventory();
				playSpellEffects(EffectPosition.DISABLED, opener, menu.data);
			}

			return;
		}

		Player targetPlayer = target.getPlayer();
		if (targetPlayer == null) return;

		SpellData targetData = menu.openerData.target(targetPlayer);

		meta.displayName(translate(opener, targetPlayer, skullName, targetData));
		item.setItemMeta(meta);

		if (menu.radius > 0 && targetPlayer.getLocation().distance(opener.getLocation()) > menu.radius) {
			meta.displayName(translate(opener, targetPlayer, skullNameRadius, targetData));
			if (spellRange != null) spellRange.subcast(menu.openerData);

			if (menu.stayOpen) item.setItemMeta(meta);
			else {
				opener.closeInventory();
				playSpellEffects(EffectPosition.DISABLED, opener, menu.data);
			}

			return;
		}

		switch (event.getClick()) {
			case LEFT -> processClickSpell(spellOnLeft, targetPlayer, menu);
			case RIGHT -> processClickSpell(spellOnRight, targetPlayer, menu);
			case MIDDLE -> processClickSpell(spellOnMiddle, targetPlayer, menu);
			case SHIFT_LEFT -> processClickSpell(spellOnSneakLeft, targetPlayer, menu);
			case SHIFT_RIGHT -> processClickSpell(spellOnSneakRight, targetPlayer, menu);
		}

		String variableTarget = this.variableTarget.get(menu.data);
		if (variableTarget != null && !variableTarget.isEmpty() && MagicSpells.getVariableManager().getVariable(variableTarget) != null)
			MagicSpells.getVariableManager().set(variableTarget, opener, target.getName());

		if (menu.stayOpen) open(opener, menu);
		else {
			opener.closeInventory();
			playSpellEffects(EffectPosition.DISABLED, opener, menu.data);
		}
	}

	private class PlayerMenuInventory implements InventoryHolder {

		private Inventory inventory;
		private int page;

		private final SpellData data;
		private final SpellData openerData;

		private final boolean stayOpen;
		private final boolean addOpener;
		private final boolean castSpellsOnTarget;

		private final double radius;

		public PlayerMenuInventory(SpellData data) {
			this.data = data;

			stayOpen = PlayerMenuSpell.this.stayOpen.get(data);
			addOpener = PlayerMenuSpell.this.addOpener.get(data);
			castSpellsOnTarget = PlayerMenuSpell.this.castSpellsOnTarget.get(data);

			radius = PlayerMenuSpell.this.radius.get(data);

			openerData = data.builder().caster(data.target()).target(null).build();
		}

		private void createInventory(int size) {
			inventory = Bukkit.createInventory(this, size, title.get(data));
		}

		private PlayerMenuSpell getSpell() {
			return PlayerMenuSpell.this;
		}

		@Override
		@NotNull
		public Inventory getInventory() {
			return inventory;
		}

	}

}
