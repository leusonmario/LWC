/*
 * Copyright (c) 2011-2013 Tyler Blair
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ''AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and contributors and should not be interpreted as representing official policies,
 * either expressed or implied, of anybody else.
 */

package org.getlwc;

import org.getlwc.component.LocationSetComponent;
import org.getlwc.component.RoleSetComponent;
import org.getlwc.configuration.Configuration;
import org.getlwc.content.role.PlayerRole;
import org.getlwc.content.role.PlayerRoleFactory;
import org.getlwc.model.Protection;
import org.getlwc.role.RoleCreationException;
import org.getlwc.role.RoleRegistry;
import org.getlwc.role.SimpleRoleRegistry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SimpleProtectionManager implements ProtectionManager {

    /**
     * The LWC engine instance
     */
    private Engine engine;

    /**
     * The role registry
     */
    private final RoleRegistry roleRegistry = new SimpleRoleRegistry();

    public SimpleProtectionManager(Engine engine) {
        this.engine = engine;

        roleRegistry.registerRoleLoader(PlayerRole.TYPE, new PlayerRoleFactory(engine));
    }

    @Override
    public boolean isBlockProtectable(Block block) {
        String enabled = getProtectionConfiguration("enabled", block.getName(), Integer.toString(block.getType()));
        return enabled.equalsIgnoreCase("true") || enabled.equalsIgnoreCase("yes");
    }

    @Override
    public Protection loadProtection(Location location) {
        return engine.getDatabase().loadProtection(location);
    }

    @Override
    public Protection createProtection(UUID owner, Location location) {
        // First create the protection
        Protection protection = engine.getDatabase().createProtection();

        // ensure it was created
        if (protection == null) {
            System.out.println("createProtection returned null!");
            return null;
        }

        // Add all linked blocks to the protection
        LocationSetComponent locationSet = new LocationSetComponent();

        for (Location foundLocation : getBlocksForPossibleProtection(location)) {
            locationSet.add(foundLocation);
        }

        protection.addComponent(locationSet);

        // add the Owner role to the database for the player
        try {
            PlayerRole role = roleRegistry.loadRole(PlayerRole.TYPE, owner.toString());
            role.setAccess(Protection.Access.OWNER);

            protection.getComponent(RoleSetComponent.class).add(role);
        } catch (RoleCreationException e) {
            System.out.println("Failed to attach owner to protection: " + e.getMessage());
            protection.remove();
            return null;
        }

        protection.save();

        return protection;
    }

    @Override
    public RoleRegistry getRoleRegistry() {
        return roleRegistry;
    }

    /**
     * Gets all blocks that may be part of a given protection, if it were protected (or not).
     * i.e. double chest, 2 parts of a door + the block under it, etc.
     *
     * @param location
     * @return
     */
    private Set<Location> getBlocksForPossibleProtection(Location location) {
        Set<Location> result = new HashSet<>();

        ProtectionMatcher matcher = new SimpleProtectionMatcher();
        Set<Block> blocks = matcher.matchBlocks(location.getBlock());

        for (Block block : blocks) {
            result.add(block.getLocation());
        }

        return result;
    }

    /**
     * Get protection configuration
     *
     * @param node
     * @param match a list of strings that can be matched. e.g [ chest, 54 ] -> will match protections.protectables.chest and protections.protectables.54
     * @return
     */
    private String getProtectionConfiguration(String node, String... match) {
        Configuration configuration = engine.getConfiguration();

        String value = null;

        // try highest nodes first
        for (String m : match) {
            // period (.) in the match name is a special case
            if (m.contains(".")) {
                Map<String, Object> map = (Map<String, Object>) configuration.get("protections.protectables");

                if (map.containsKey(m)) {
                    Map<String, Object> sub = (Map<String, Object>) map.get(m);

                    if (sub.containsKey(node)) {
                        value = sub.get(node).toString();
                    }
                }
            } else {
                value = configuration.getString("protections.protectables." + m + "." + node, null);
            }

            if (value != null) {
                break;
            }
        }

        if (value == null) {
            // try the defaults
            value = configuration.getString("protections." + node, "");
        }

        return value;
    }

}
