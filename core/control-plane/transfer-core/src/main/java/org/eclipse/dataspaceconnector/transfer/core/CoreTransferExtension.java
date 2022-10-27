/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - improvements
 *
 */

package org.eclipse.dataspaceconnector.transfer.core;

import org.eclipse.dataspaceconnector.common.statemachine.retry.EntitySendRetryManager;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.CoreExtension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Provides;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Setting;
import org.eclipse.dataspaceconnector.spi.asset.DataAddressResolver;
import org.eclipse.dataspaceconnector.spi.command.BoundedCommandQueue;
import org.eclipse.dataspaceconnector.spi.command.CommandHandlerRegistry;
import org.eclipse.dataspaceconnector.spi.command.CommandRunner;
import org.eclipse.dataspaceconnector.spi.event.EventRouter;
import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.spi.policy.engine.PolicyEngine;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyArchive;
import org.eclipse.dataspaceconnector.spi.retry.ExponentialWaitStrategy;
import org.eclipse.dataspaceconnector.spi.security.Vault;
import org.eclipse.dataspaceconnector.spi.system.ExecutorInstrumentation;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.transfer.TransferProcessManager;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceReceiverRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.edr.EndpointDataReferenceTransformerRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.transfer.observe.TransferProcessObservable;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ProvisionManager;
import org.eclipse.dataspaceconnector.spi.transfer.provision.ResourceManifestGenerator;
import org.eclipse.dataspaceconnector.spi.transfer.retry.TransferWaitStrategy;
import org.eclipse.dataspaceconnector.spi.transfer.status.StatusCheckerRegistry;
import org.eclipse.dataspaceconnector.spi.transfer.store.TransferProcessStore;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.ProvisionedContentResource;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.command.TransferProcessCommand;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.AddProvisionedResourceCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.command.handlers.DeprovisionCompleteCommandHandler;
import org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceReceiverRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.edr.EndpointDataReferenceTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.flow.DataFlowManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.listener.TransferProcessEventListener;
import org.eclipse.dataspaceconnector.transfer.core.observe.TransferProcessObservableImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ProvisionManagerImpl;
import org.eclipse.dataspaceconnector.transfer.core.provision.ResourceManifestGeneratorImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.StatusCheckerRegistryImpl;
import org.eclipse.dataspaceconnector.transfer.core.transfer.TransferProcessManagerImpl;

import java.time.Clock;

/**
 * Provides core data transfer services to the system.
 */
@CoreExtension
@Provides({ StatusCheckerRegistry.class, ResourceManifestGenerator.class, TransferProcessManager.class,
        TransferProcessObservable.class, DataFlowManager.class, ProvisionManager.class,
        EndpointDataReferenceReceiverRegistry.class, EndpointDataReferenceTransformerRegistry.class })
@Extension(value = CoreTransferExtension.NAME)
public class CoreTransferExtension implements ServiceExtension {

    public static final String NAME = "Core Transfer";

    private static final long DEFAULT_ITERATION_WAIT = 5000;

    @Setting(value = "the iteration wait time in milliseconds on the state machine. Default value 5000")
    private static final String TRANSFER_STATE_MACHINE_ITERATION_WAIT_MILIS = "edc.transfer.state-machine.iteration-wait-millis";

    @Setting
    private static final String TRANSFER_STATE_MACHINE_BATCH_SIZE = "edc.transfer.state-machine.batch-size";

    @Setting
    private static final String TRANSFER_SEND_RETRY_LIMIT = "edc.transfer.send.retry.limit";

    @Setting
    private static final String TRANSFER_SEND_RETRY_BASE_DELAY_MS = "edc.transfer.send.retry.base-delay.ms";

    @Inject
    private TransferProcessStore transferProcessStore;

    @Inject
    private PolicyArchive policyArchive;

    @Inject
    private CommandHandlerRegistry registry;

    @Inject
    private RemoteMessageDispatcherRegistry dispatcherRegistry;

    @Inject
    private DataAddressResolver addressResolver;

    @Inject
    private Vault vault;

    @Inject
    private EventRouter eventRouter;

    @Inject
    private Clock clock;

    @Inject
    private PolicyEngine policyEngine;

    private TransferProcessManagerImpl processManager;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();

        var telemetry = context.getTelemetry();

        var typeManager = context.getTypeManager();

        registerTypes(typeManager);

        var dataFlowManager = new DataFlowManagerImpl();
        context.registerService(DataFlowManager.class, dataFlowManager);

        var manifestGenerator = new ResourceManifestGeneratorImpl(policyEngine);
        context.registerService(ResourceManifestGenerator.class, manifestGenerator);

        var statusCheckerRegistry = new StatusCheckerRegistryImpl();
        context.registerService(StatusCheckerRegistry.class, statusCheckerRegistry);

        var provisionManager = new ProvisionManagerImpl(monitor);
        context.registerService(ProvisionManager.class, provisionManager);

        var iterationWaitMillis = context.getSetting(TRANSFER_STATE_MACHINE_ITERATION_WAIT_MILIS, DEFAULT_ITERATION_WAIT);
        var waitStrategy = context.hasService(TransferWaitStrategy.class) ? context.getService(TransferWaitStrategy.class) : new ExponentialWaitStrategy(iterationWaitMillis);

        var endpointDataReferenceReceiverRegistry = new EndpointDataReferenceReceiverRegistryImpl();
        context.registerService(EndpointDataReferenceReceiverRegistry.class, endpointDataReferenceReceiverRegistry);

        // Register a default EndpointDataReferenceTransformer that can be overridden in extensions.
        var endpointDataReferenceTransformerRegistry = new EndpointDataReferenceTransformerRegistryImpl();
        context.registerService(EndpointDataReferenceTransformerRegistry.class, endpointDataReferenceTransformerRegistry);

        var commandQueue = new BoundedCommandQueue<TransferProcessCommand>(10);
        var observable = new TransferProcessObservableImpl();
        context.registerService(TransferProcessObservable.class, observable);

        observable.registerListener(new TransferProcessEventListener(eventRouter, clock));

        var retryLimit = context.getSetting(TRANSFER_SEND_RETRY_LIMIT, 7);
        var retryBaseDelay = context.getSetting(TRANSFER_SEND_RETRY_BASE_DELAY_MS, 100L);
        Clock clock = context.getClock();
        var sendRetryManager = new EntitySendRetryManager(monitor, () -> new ExponentialWaitStrategy(retryBaseDelay), clock, retryLimit);

        processManager = TransferProcessManagerImpl.Builder.newInstance()
                .waitStrategy(waitStrategy)
                .manifestGenerator(manifestGenerator)
                .dataFlowManager(dataFlowManager)
                .provisionManager(provisionManager)
                .dispatcherRegistry(dispatcherRegistry)
                .statusCheckerRegistry(statusCheckerRegistry)
                .monitor(monitor)
                .telemetry(telemetry)
                .executorInstrumentation(context.getService(ExecutorInstrumentation.class))
                .vault(vault)
                .clock(clock)
                .typeManager(typeManager)
                .commandQueue(commandQueue)
                .commandRunner(new CommandRunner<>(registry, monitor))
                .observable(observable)
                .transferProcessStore(transferProcessStore)
                .policyArchive(policyArchive)
                .batchSize(context.getSetting(TRANSFER_STATE_MACHINE_BATCH_SIZE, 5))
                .sendRetryManager(sendRetryManager)
                .addressResolver(addressResolver)
                .build();

        context.registerService(TransferProcessManager.class, processManager);

        registry.register(new AddProvisionedResourceCommandHandler(processManager));
        registry.register(new DeprovisionCompleteCommandHandler(processManager));
    }

    @Override
    public void start() {
        processManager.start();
    }

    @Override
    public void shutdown() {
        if (processManager != null) {
            processManager.stop();
        }
    }

    private void registerTypes(TypeManager typeManager) {
        typeManager.registerTypes(DataRequest.class);
        typeManager.registerTypes(ProvisionedContentResource.class);
        typeManager.registerTypes(DeprovisionedResource.class);
    }
}
