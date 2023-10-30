package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.Spell;

public class SpellForcetossEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Spell spell;
    private final LivingEntity caster;
    private final LivingEntity target;
    private Vector velocity;
    private boolean cancelled;

    public SpellForcetossEvent(Spell spell, LivingEntity caster, LivingEntity target, Vector velocity) {
        this.spell = spell;
        this.caster = caster;
        this.target = target;
        this.velocity = velocity;
        cancelled = false;
    }

    public LivingEntity getCaster() {
        return this.caster;
    }

    public Spell getSpell() {
        return this.spell;
    }

    public LivingEntity getTarget() {
        return this.target;
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