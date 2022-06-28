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
import java.util.Objects;

/**
 * Base class for entities.
 */
public abstract class Entity<T extends Entity<T>> {

    protected String id;
    protected long createdTimestamp;

    protected Entity() {
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getId() {
        return id;
    }

    protected abstract static class Builder<T extends StatefulEntity<T>, B extends StatefulEntity.Builder<T, B>> {

        protected final T entity;

        protected Builder(T entity) {
            this.entity = entity;
        }

        public abstract B self();

        public B id(String id) {
            entity.id = id;
            return self();
        }

        public B createdTimestamp(long value) {
            entity.createdTimestamp = value;
            return self();
        }

        protected T build() {
            Objects.requireNonNull(entity.id, "id");
            entity.clock = Objects.requireNonNullElse(entity.clock, Clock.systemUTC());
            if (entity.stateTimestamp == 0) {
                entity.stateTimestamp = entity.clock.millis();
            }
            return entity;
        }
    }

}
