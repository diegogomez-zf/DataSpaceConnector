/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *       Microsoft Corporation - added fields
 *
 */

package org.eclipse.dataspaceconnector.spi.entity;


import java.time.Clock;

/**
 * Base class for entities.
 */
public abstract class Entity<T extends Entity<T>> {

    protected String id;
    protected long createdTimestamp;
    protected long stateTimestamp;
    protected Clock clock;

    protected Entity() {
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public String getId() {
        return id;
    }

    public void updateStateTimestamp() {
        stateTimestamp = clock.millis();
    }

}
