package org.getlwc.entity;

import org.getlwc.Engine;
import org.getlwc.Location;

public class WorkbenchPlayer extends Player {

    /**
     * The LWC engine instance
     */
    private Engine engine;

    /**
     * Player handle
     */
    private net.minecraft.workbench.server.players.Player handle;

    public WorkbenchPlayer(Engine engine, net.minecraft.workbench.server.players.Player handle) {
        this.engine = engine;
        this.handle = handle;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return handle.getUsername();
    }

    /**
     * {@inheritDoc}
     */
    public Location getLocation() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public void sendMessage(String message) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPermission(String node) {
        throw new UnsupportedOperationException("Not implemented");
    }

}