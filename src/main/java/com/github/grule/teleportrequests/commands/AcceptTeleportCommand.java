package com.github.grule.teleportrequests.commands;

import com.github.grule.teleportrequests.TeleportRequests;
import com.github.grule.teleportrequests.TeleportRequest;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.UUID;

public class AcceptTeleportCommand extends AbstractPlayerCommand {

    private static final Message MESSAGE_NO_PENDING_REQUEST = Message.translation("teleport_requests.commands.tpaccept.none").color(new Color(0xff3333));
    private static final Message MESSAGE_TELEPORT_ACCEPTED = Message.translation("teleport_requests.commands.tpaccept.success.target").color(Color.GREEN);
    private static final Message MESSAGE_TELEPORT_ACCEPTED_REQUESTER = Message.translation("teleport_requests.commands.tpaccept.success.requester").color(Color.GREEN);
    private static final Message MESSAGE_TELEPORT_HERE_ACCEPTED = Message.translation("teleport_requests.commands.tpaccept.success.tphere_requester").color(Color.GREEN);
    private static final Message MESSAGE_TELEPORT_FAILED = Message.translation("teleport_requests.commands.tpaccept.failed").color(new Color(0xff3333));

    @Nonnull
    private final OptionalArg<PlayerRef> requesterPlayerArg;
    @Nonnull
    private final TeleportRequests plugin;

    public AcceptTeleportCommand() {
        super("tpaccept", "teleport_requests.commands.tpaccept.desc");
        this.plugin = TeleportRequests.get();
        this.setPermissionGroup(GameMode.Adventure);

        this.requesterPlayerArg = this.withOptionalArg("requesterPlayer", "teleport_requests.commands.tpaccept.args.requesterPlayer", ArgTypes.PLAYER_REF);
    }

    @Override
    protected void execute(@NotNull CommandContext context, @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> executorRef, @NotNull PlayerRef executorPlayerRef, @NotNull World world) {
        UUID executorUuid = executorPlayerRef.getUuid();
        PlayerRef argumentPlayer = this.requesterPlayerArg.get(context);

        TeleportRequest request;
        if (argumentPlayer == null) {
            request = this.plugin.findRequestForTarget(executorUuid);
        } else {
            request = this.plugin.findRequestFromRequester(argumentPlayer.getUuid());
            // Verify the request is actually for the current player
            if (request != null && !request.getTarget().equals(executorUuid)) {
                request = null;
            }
        }

        if (request == null) {
            executorPlayerRef.sendMessage(MESSAGE_NO_PENDING_REQUEST);
            return;
        }

        if (request.isExpired()) {
            plugin.removeRequest(request.getRequester());
            executorPlayerRef.sendMessage(MESSAGE_NO_PENDING_REQUEST);
            return;
        }

        var result = request.execute();

        if (!result) {
            executorPlayerRef.sendMessage(MESSAGE_TELEPORT_FAILED);
            return;
        }

        // Get requester PlayerRef for messages (should be valid since execute() just succeeded)
        var requesterPlayerRef = getPlayerRefByUuid(request.getRequester());
        if (requesterPlayerRef == null) {
            // Unlikely race condition - teleport succeeded but player disconnected before messages
            plugin.removeRequest(request.getRequester());
            return;
        }

        if (request.getType() == TeleportRequest.Type.TELEPORT_TO) {
            // Requester teleported to acceptor
            requesterPlayerRef.sendMessage(MESSAGE_TELEPORT_ACCEPTED_REQUESTER.param("username", executorPlayerRef.getUsername()));
            executorPlayerRef.sendMessage(MESSAGE_TELEPORT_ACCEPTED.param("username", requesterPlayerRef.getUsername()));
        } else if (request.getType() == TeleportRequest.Type.TELEPORT_HERE) {
            // Acceptor teleported to requester
            executorPlayerRef.sendMessage(MESSAGE_TELEPORT_ACCEPTED_REQUESTER.param("username", requesterPlayerRef.getUsername()));
            requesterPlayerRef.sendMessage(MESSAGE_TELEPORT_HERE_ACCEPTED.param("username", executorPlayerRef.getUsername()));
        }

        plugin.removeRequest(request.getRequester());
    }

    @Nullable
    private PlayerRef getPlayerRefByUuid(UUID uuid) {
        return Universe.get().getPlayer(uuid);
    }
}
