/*
 * Copyright (c) 2022 ZF Friedrichshafen AG
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Contributors:
 *    ZF Friedrichshafen AG - Initial API and Implementation
 */

package org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractAgreementTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var agreement = ContractAgreement.Builder.newInstance()
                .id("1")
                .createdTimestamp(0L)
                .providerAgentId("agent")
                .consumerAgentId("consumer")
                .contractSigningDate(20150212)
                .contractStartDate(20220627)
                .contractEndDate(20230101)
                .assetId("associated-asset")
                .policy(Policy.Builder.newInstance().build())
                .build();

        var serialized = mapper.writeValueAsString(agreement);
        var deserialized = mapper.readValue(serialized, ContractAgreement.class);

        assertThat(deserialized).isNotNull();
    }
}
