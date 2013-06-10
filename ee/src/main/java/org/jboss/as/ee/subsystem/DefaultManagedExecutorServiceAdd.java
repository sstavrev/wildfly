/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultManagedExecutorServiceProcessor;
import org.jboss.as.ee.concurrent.service.ConcurrentServiceNames;
import org.jboss.as.ee.concurrent.service.ManagedExecutorServiceService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Eduardo Martins
 */
public class DefaultManagedExecutorServiceAdd extends AbstractBoottimeAddStepHandler {

    static final DefaultManagedExecutorServiceAdd INSTANCE = new DefaultManagedExecutorServiceAdd();

    private DefaultManagedExecutorServiceAdd() {
        super(DefaultManagedExecutorServiceResourceDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final long hungTaskThreshold = DefaultManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD_AD.resolveModelAttribute(context, model).asLong();
        final boolean longRunningTasks = DefaultManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS_AD.resolveModelAttribute(context, model).asBoolean();
        final int coreThreads = DefaultManagedExecutorServiceResourceDefinition.CORE_THREADS_AD.resolveModelAttribute(context, model).asInt();
        final int maxThreads = DefaultManagedExecutorServiceResourceDefinition.MAX_THREADS_AD.resolveModelAttribute(context, model).asInt();
        final long keepAliveTime = DefaultManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME_AD.resolveModelAttribute(context, model).asLong();
        final TimeUnit keepAliveTimeUnit = TimeUnit.MILLISECONDS;
        final long threadLifeTime = 0L;
        final int queueLength = DefaultManagedExecutorServiceResourceDefinition.QUEUE_LENGTH_AD.resolveModelAttribute(context, model).asInt();
        final AbstractManagedExecutorService.RejectPolicy rejectPolicy = AbstractManagedExecutorService.RejectPolicy.valueOf(DefaultManagedExecutorServiceResourceDefinition.REJECT_POLICY_AD.resolveModelAttribute(context, model).asString());

        final ManagedExecutorServiceService service = new ManagedExecutorServiceService(ConcurrentServiceNames.DEFAULT_NAME, hungTaskThreshold, longRunningTasks, coreThreads, maxThreads, keepAliveTime, keepAliveTimeUnit, threadLifeTime, queueLength, rejectPolicy);
        final ServiceBuilder serviceBuilder = context.getServiceTarget().addService(ConcurrentServiceNames.DEFAULT_MANAGED_EXECUTOR_SERVICE_SERVICE_NAME, service)
                .addDependency(ConcurrentServiceNames.DEFAULT_CONTEXT_SERVICE_SERVICE_NAME, ContextServiceImpl.class, service.getContextServiceInjector())
                .addListener(verificationHandler);
        newControllers.add(serviceBuilder.install());

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_CONCURRENT_DEFAULT_MANAGED_EXECUTOR_SERVICE, new EEConcurrentDefaultManagedExecutorServiceProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
