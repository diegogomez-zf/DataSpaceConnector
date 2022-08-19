/*
 *  Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.framework.store.InMemoryDataPlaneStore;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.dataplane.spi.registry.TransferServiceRegistry;
import org.eclipse.dataspaceconnector.dataplane.spi.store.DataPlaneStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataPlaneManagerImplTest {
    TransferService transferService = mock(TransferService.class);
    DataPlaneStore store = new InMemoryDataPlaneStore(10);
    DataFlowRequest request = createRequest();
    TransferServiceRegistry registry = mock(TransferServiceRegistry.class);
    DataPlaneManagerImpl dataPlaneManager = createDataPlaneManager();

    @BeforeEach
    public void setUp() {
        dataPlaneManager.start();
        when(registry.resolveTransferService(request))
                .thenReturn(transferService);
    }

    /**
     * Verifies a request is enqueued, dequeued, and dispatched to the pipeline service.
     */
    @Test
    void verifyWorkDispatch() {

        when(registry.resolveTransferService(request))
                .thenReturn(transferService);
        when(transferService.canHandle(isA(DataFlowRequest.class)))
                .thenReturn(true);

        when(transferService.transfer(isA(DataFlowRequest.class))).thenAnswer(i -> {
            return completedFuture(Result.success("ok"));
        });

        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> {
            verify(registry).resolveTransferService(eq(request));
            verify(transferService).transfer(isA(DataFlowRequest.class));
        });
    }

    /**
     * Verifies that the dispatch thread survives an error thrown by a worker.
     */
    @Test
    void verifyWorkDispatchError() {

        when(transferService.canHandle(request))
                .thenReturn(true);

        when(transferService.transfer(request))
                .thenThrow(new RuntimeException("Test exception"))
                .thenAnswer((i -> completedFuture(Result.success("ok"))));

        dataPlaneManager.start();

        dataPlaneManager.initiateTransfer(request);
        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> verify(transferService, times(2)).transfer(request));
    }

    @Test
    void verifyWorkDispatch_onUnavailableTransferService_completesTransfer() {

        when(transferService.canHandle(isA(DataFlowRequest.class)))
                .thenReturn(false);

        dataPlaneManager.initiateTransfer(request);

        await().untilAsserted(() -> assertThat(store.getState(request.getProcessId())).isEqualTo(DataPlaneStore.State.COMPLETED));
    }

    DataFlowRequest createRequest() {
        return DataFlowRequest.Builder.newInstance()
                .id("1")
                .processId("1")
                .sourceDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .destinationDataAddress(DataAddress.Builder.newInstance().type("type").build())
                .build();
    }

    private DataPlaneManagerImpl createDataPlaneManager() {
        return DataPlaneManagerImpl.Builder.newInstance()
                .queueCapacity(100)
                .workers(1)
                .executorInstrumentation(ExecutorInstrumentation.noop())
                .waitTimeout(10)
                .transferServiceRegistry(registry)
                .store(store)
                .monitor(mock(Monitor.class))
                .build();
    }

}
