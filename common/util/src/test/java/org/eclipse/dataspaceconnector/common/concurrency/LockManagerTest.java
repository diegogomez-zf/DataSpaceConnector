/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.common.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class LockManagerTest {

    @Test
    void verifyReadAndWriteLock() {
        var lockManager = new LockManager(new ReentrantReadWriteLock());
        var counter = new AtomicInteger();

        lockManager.readLock(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);

        lockManager.writeLock(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void verifyTimeoutOnWriteLockAttempt() {
        var lockManager = new LockManager(new ReentrantReadWriteLock(), 10);
        var counter = new AtomicInteger();

        // Attempt to acquire a write lock in another thread, which should timeout as the current thread holds a read lock
        var thread = new Thread(() -> {
            lockManager.writeLock(() -> {
                throw new AssertionError();  // lock should never be acquired
            });
        });

        lockManager.readLock(() -> {
            thread.start();
            counter.incrementAndGet();
            return null;
        });

        await().untilAsserted(() -> assertThat(counter.get()).isEqualTo(1));
    }
}
