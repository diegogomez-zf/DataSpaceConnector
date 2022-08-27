/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

plugins {
    `java-library`
    `java-test-fixtures`
    `maven-publish`
}

val awaitility: String by project

dependencies {
    api(project(":spi:common:core-spi"))
    testImplementation("org.awaitility:awaitility:${awaitility}")
}

publishing {
    publications {
        create<MavenPublication>("state-machine-lib") {
            artifactId = "state-machine-lib"
            from(components["java"])
        }
    }
}
