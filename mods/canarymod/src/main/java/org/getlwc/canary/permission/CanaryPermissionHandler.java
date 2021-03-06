package org.getlwc.canary.permission;

import net.canarymod.Canary;
import net.canarymod.user.Group;
import org.getlwc.entity.Player;
import org.getlwc.permission.PermissionHandler;

import java.util.HashSet;
import java.util.Set;

public class CanaryPermissionHandler implements PermissionHandler {

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasPermission(Player player, String node) {
        net.canarymod.api.entity.living.humanoid.Player handle = Canary.getServer().getPlayer(player.getName());
        return handle != null && handle.hasPermission(node);
    }

    @Override
    public Set<String> getGroups(Player player) {
        Set<String> groups = new HashSet<String>();

        net.canarymod.api.entity.living.humanoid.Player handle = Canary.getServer().getPlayer(player.getName());

        for (Group group : handle.getPlayerGroups()) {
            groups.add(group.getName());
        }

        return groups;
    }

}
