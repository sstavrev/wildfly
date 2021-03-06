/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common.elytron;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.elytron.ElytronExtension;

/**
 * Utility methods to create/remove simple security domains
 *
 * @author <a href="mailto:jkalina@redhat.com">Jan Kalina</a>
 */
public class EjbElytronDomainSetup extends AbstractSecurityDomainSetup {

    private static final String DEFAULT_SECURITY_DOMAIN_NAME = "ejb3-tests";

    private PathAddress realmAddress;

    private PathAddress domainAddress;

    private PathAddress saslAuthenticationAddress;

    private PathAddress remotingConnectorAddress;

    private PathAddress ejbDomainAddress;

    private PathAddress ejbRemoteAddress = PathAddress.pathAddress()
            .append(SUBSYSTEM, "ejb3")
            .append("service", "remote");

    private PathAddress httpAuthenticationAddress;

    private PathAddress undertowDomainAddress;

    private final String usersFile;
    private final String groupsFile;
    private final String securityDomainName;

    public EjbElytronDomainSetup(final String usersFile, final String groupsFile) {
        this(usersFile, groupsFile, DEFAULT_SECURITY_DOMAIN_NAME);
    }

    public EjbElytronDomainSetup(final String usersFile, final String groupsFile, final String securityDomainName) {
        this.usersFile = usersFile;
        this.groupsFile = groupsFile;
        this.securityDomainName = securityDomainName;
    }

    protected String getSecurityDomainName() {
        return securityDomainName;
    }

    protected String getSecurityRealmName() {
        return getSecurityDomainName() + "-ejb3-UsersRoles";
    }

    protected String getUndertowDomainName() {
        return getSecurityDomainName();
    }

    protected String getEjbDomainName() {
        return getSecurityDomainName();
    }

    protected String getSaslAuthenticationName() {
        return getSecurityDomainName();
    }

    protected String getRemotingConnectorName() {
        return getSecurityDomainName();
    }

    protected String getHttpAuthenticationName() {
        return getSecurityDomainName();
    }

    protected String getUsersFile() {
        return usersFile;
    }

    protected String getGroupsFile() {
        return groupsFile;
    }

    protected boolean isUsersFilePlain() {
        return true;
    }

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        System.out.println("elytron setup...");

        realmAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("properties-realm", getSecurityRealmName());

        domainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("security-domain", getSecurityDomainName());

        saslAuthenticationAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("sasl-authentication-factory", getSaslAuthenticationName());

        remotingConnectorAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "remoting")
                .append("http-connector", getRemotingConnectorName());

        ejbDomainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "ejb3")
                .append("application-security-domain", getEjbDomainName());

        httpAuthenticationAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, ElytronExtension.SUBSYSTEM_NAME)
                .append("http-authentication-factory", getHttpAuthenticationName());

        undertowDomainAddress = PathAddress.pathAddress()
                .append(SUBSYSTEM, "undertow")
                .append("application-security-domain", getUndertowDomainName());

        final ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(ModelDescriptionConstants.COMPOSITE);
        compositeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        compositeOp.get(OP_ADDR).setEmptyList();

        ModelNode steps = compositeOp.get(STEPS);

        // /subsystem=elytron/properties-realm=UsersRoles:add(users-properties={path=users.properties},groups-properties={path=roles.properties})
        ModelNode addRealm = Util.createAddOperation(realmAddress);
        addRealm.get("users-properties").get("path").set(getUsersFile());
        addRealm.get("users-properties").get("plain-text").set(isUsersFilePlain()); // not hashed
        addRealm.get("groups-properties").get("path").set(getGroupsFile());
        steps.add(addRealm);

        // /subsystem=elytron/security-domain=EjbDomain:add(default-realm=UsersRoles, realms=[{realm=UsersRoles}])
        ModelNode addDomain = Util.createAddOperation(domainAddress);
        addDomain.get("permission-mapper").set("default-permission-mapper"); // LoginPermission for everyone (defined in standalone-elytron.xml)
        addDomain.get("default-realm").set(getSecurityRealmName());
        addDomain.get("realms").get(0).get("realm").set(getSecurityRealmName());
        addDomain.get("realms").get(0).get("role-decoder").set("groups-to-roles"); // use attribute "groups" as roles (defined in standalone-elytron.xml)
        steps.add(addDomain);

        // /subsystem=elytron/sasl-authentication-factory=ejb3-tests-auth-fac:add(sasl-server-factory=configured,security-domain=EjbDomain,mechanism-configurations=[{mechanism-name=BASIC}])
        ModelNode addSaslAuthentication = Util.createAddOperation(saslAuthenticationAddress);
        addSaslAuthentication.get("sasl-server-factory").set("configured");
        addSaslAuthentication.get("security-domain").set(getSecurityDomainName());
        addSaslAuthentication.get("mechanism-configurations").get(0).get("mechanism-name").set("BASIC");
        steps.add(addSaslAuthentication);

        // remoting connection with sasl-authentication-factory
        ModelNode addRemotingConnector = Util.createAddOperation(remotingConnectorAddress);
        addRemotingConnector.get("sasl-authentication-factory").set(getSaslAuthenticationName());
        addRemotingConnector.get("connector-ref").set("default");
        // authentication-provider  sasl-protocol  security-realm  server-name
        steps.add(addRemotingConnector);

        // /subsystem=ejb3/application-security-domain=ejb3-tests:add(security-domain=ApplicationDomain)
        ModelNode addEjbDomain = Util.createAddOperation(ejbDomainAddress);
        addEjbDomain.get("security-domain").set(getSecurityDomainName());
        steps.add(addEjbDomain);

        steps.add(Util.getWriteAttributeOperation(ejbRemoteAddress, "connector-ref", getSecurityDomainName()));

        ModelNode addHttpAuthentication = Util.createAddOperation(httpAuthenticationAddress);
        addHttpAuthentication.get("security-domain").set(getSecurityDomainName());
        addHttpAuthentication.get("http-server-mechanism-factory").set("global");
        addHttpAuthentication.get("mechanism-configurations").get(0).get("mechanism-name").set("BASIC");
        addHttpAuthentication.get("mechanism-configurations").get(0).get("mechanism-realm-configurations").get(0).get("realm-name").set("TestingRealm");
        steps.add(addHttpAuthentication);

        ModelNode addUndertowDomain = Util.createAddOperation(undertowDomainAddress);
        addUndertowDomain.get("http-authentication-factory").set(getHttpAuthenticationName());
        steps.add(addUndertowDomain);

        applyUpdate(managementClient.getControllerClient(), compositeOp, false);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) {
        System.out.println("tearing down...");

        List<ModelNode> updates = new LinkedList<>();
        updates.add(Util.createRemoveOperation(undertowDomainAddress));
        updates.add(Util.createRemoveOperation(httpAuthenticationAddress));
        updates.add(Util.getWriteAttributeOperation(ejbRemoteAddress, "connector-ref", "http-remoting-connector"));
        updates.add(Util.createRemoveOperation(ejbDomainAddress));
        updates.add(Util.createRemoveOperation(remotingConnectorAddress));
        updates.add(Util.createRemoveOperation(saslAuthenticationAddress));
        updates.add(Util.createRemoveOperation(domainAddress));
        updates.add(Util.createRemoveOperation(realmAddress));

        applyUpdates(managementClient.getControllerClient(), updates, false);
    }

    protected static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates, boolean allowFailure) {
        ModelNode compositeOp = new ModelNode();
        compositeOp.get(OP).set(COMPOSITE);
        compositeOp.get(OP_ADDR).setEmptyList();
        compositeOp.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        ModelNode steps = compositeOp.get(STEPS);
        for (ModelNode update : updates) {
            steps.add(update);
        }
        try {
            applyUpdate(client, compositeOp, allowFailure);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
