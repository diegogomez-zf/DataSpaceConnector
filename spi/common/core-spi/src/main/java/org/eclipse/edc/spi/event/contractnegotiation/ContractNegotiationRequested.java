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

package org.eclipse.edc.spi.event.contractnegotiation;

import org.eclipse.edc.spi.event.Event;

import java.util.Objects;

/**
 * This event is raised when the ContractNegotiation has been requested.
 */
public class ContractNegotiationRequested extends Event<ContractNegotiationRequested.Payload> {

    private ContractNegotiationRequested() {
    }

    public static class Builder extends Event.Builder<ContractNegotiationRequested, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new ContractNegotiationRequested(), new Payload());
        }

        public Builder contractNegotiationId(String contractNegotiationId) {
            event.payload.contractNegotiationId = contractNegotiationId;
            return this;
        }

        @Override
        protected void validate() {
            Objects.requireNonNull(event.payload.contractNegotiationId);
        }
    }

    /**
     * This class contains all event specific attributes of a ContractNegotiation Requested Event
     *
     */
    public static class Payload extends ContractNegotiationEventPayload {
    }
}
