/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.messaging.activemq;

import org.jboss.as.controller.capability.RuntimeCapability;

/**
 * Capabilities for the messaging-activemq extension. This is not to be used outside of this extension.
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2017 Red Hat Inc.
 */
public class Capabilities {

    /**
     * The capability name for the JMX management.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/management/jmx/capability.adoc">Capability documentation</a>
     */
    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";

    /**
     * A capability for the current messaging-activemq server configuration.
     *
     * @see <a href="https://github.com/wildfly/wildfly-capabilities/blob/master/org/wildfly/messaging/activemq/server/capability.adoc">Capability documentation</a>
     */
    static final RuntimeCapability<Void> ACTIVEMQ_SERVER_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.messaging.activemq.server", true)
            .addOptionalRequirements(JMX_CAPABILITY)
            .build();

    /**
     * The capability name for the Elytron security domain.
     */
    static final String ELYTRON_DOMAIN_CAPABILITY = "org.wildfly.security.security-domain";
}
