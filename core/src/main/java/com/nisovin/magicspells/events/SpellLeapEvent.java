package com.nisovin.magicspells.events;

import com.nisovin.magicspells.Spell;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;

public class SpellLeapEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Spell spell;
    private final LivingEntity caster;
    private Vector velocity;
    private boolean cancelled;

    public SpellLeapEvent(Spell spell, LivingEntity caster, Vector velocity) {
        this.spell = spell;
        this.caster = caster;
        this.velocity = velocity;
        cancelled = false;
    }

    public LivingEntity getCaster() {
        return this.caster;
    }

    public Spell getSpell() {
        return this.spell;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}