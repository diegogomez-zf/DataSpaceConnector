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
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer Institute for Software and Systems Engineering - expending Event classes
 *
 */

package org.eclipse.edc.spi.event.asset;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * Describe an Asset deletion, after this has emitted, the Asset represented by the id won't be available anymore.
 */
public class AssetDeleted extends Event<AssetDeleted.Payload> {

    private AssetDeleted() {
    }

    public static class Builder extends Event.Builder<AssetDeleted, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new AssetDeleted(), new Payload());
        }

        public Builder assetId(String assetId) {
            event.payload.assetId = assetId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.getAssetId());
        }
    }

    /**
     * This class contains all event specific attributes of a Asset Deletion Event
     *
     */
    public static class Payload extends AssetEventPayload {

    }
}
