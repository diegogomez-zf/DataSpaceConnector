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

package org.eclipse.edc.spi.event.contractdefinition;

import org.eclipse.edc.spi.event.EventPayload;

/**
 *  Class as organizational between level to catch events of type PolicyDefinition to catch them together in an Event Subscriber
 *  Contains data related to contract definition events
 *
 */
public class ContractDefinitionEventPayload extends EventPayload {
    protected String contractDefinitionId;

    public String getContractDefinitionId() {
        return contractDefinitionId;
    }
}
