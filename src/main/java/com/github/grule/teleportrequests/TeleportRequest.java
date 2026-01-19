package com.github.grule.teleportrequests;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public class TeleportRequest {
    private final UUID requester;
    private final UUID target;
    private final long expiresAt;
    private final Type type;
    private boolean executed = false;

    /**
     * Represents a teleport request from one player to another.
     *
     * @param requester The UUID of the player requesting to teleport
     * @param target    The UUID of the target player of the request
     * @param expiresAt Timestamp in milliseconds when this request expires
     */
    private TeleportRequest(UUID requester, UUID target, long expiresAt, TeleportRequest.Type type) {
        this.requester = requester;
        this.target = target;
        this.expiresAt = expiresAt;
        this.type = type;
    }

    public boolean execute() {
        var universe = Universe.get();
        PlayerRef requesterPlayer = universe.getPlayer(requester);
        PlayerRef targetPlayer = universe.getPlayer(target);

        if (requesterPlayer == null || targetPlayer == null) {
            return false;
        }

        final Ref<EntityStore> playerToTeleportRef;
        final PlayerRef targetPlayerToTeleport;

        if (this.type == Type.TELEPORT_TO) {
            playerToTeleportRef = requesterPlayer.getReference();
            if (playerToTeleportRef == null) {
                return false;
            }
            targetPlayerToTeleport = targetPlayer;
        } else {
            playerToTeleportRef = targetPlayer.getReference();
            if (playerToTeleportRef == null) {
                return false;
            }
            targetPlayerToTeleport = requesterPlayer;
        }

        var targetPlayerToTeleportRef = targetPlayerToTeleport.getReference();
        if (targetPlayerToTeleportRef == null) {
            return false;
        }

        var transform = targetPlayerToTeleport.getTransform();
        playerToTeleportRef.getStore().putComponent(
                playerToTeleportRef,
                Teleport.getComponentType(),
                new Teleport(
                        targetPlayerToTeleportRef.getStore().getExternalData().getWorld(),
                        transform.getPosition(),
                        transform.getRotation()
                )
        );
        executed = true;
        return true;
    }

    public UUID getRequester() {
        return requester;
    }

    public UUID getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    /**
     * Checks if this request has expired.
     *
     * @return true if the request is past its expiration time or if the request has
     *         already been executed
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt || executed;
    }

    /**
     * Creates a new teleport request with a given timeout.
     *
     * @param requester      The UUID of the player requesting to teleport
     * @param target         The UUID of the target player of the request
     * @param timeoutSeconds How long the request is valid for
     * @return A new TeleportRequest instance
     */
    public static TeleportRequest create(UUID requester, UUID target, int timeoutSeconds, TeleportRequest.Type type) {
        long expiresAt = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        return new TeleportRequest(requester, target, expiresAt, type);
    }

    public enum Type {
        TELEPORT_TO,
        TELEPORT_HERE,
    }
}
