/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.utils.pubsub;

import javax.annotation.Nullable;

import org.eclipse.ditto.services.utils.ddata.DistributedData;
import org.eclipse.ditto.services.utils.ddata.DistributedDataConfig;
import org.eclipse.ditto.services.utils.pubsub.actors.PubSupervisor;
import org.eclipse.ditto.services.utils.pubsub.actors.SubSupervisor;
import org.eclipse.ditto.services.utils.pubsub.config.PubSubConfig;
import org.eclipse.ditto.services.utils.pubsub.ddata.DData;
import org.eclipse.ditto.services.utils.pubsub.ddata.compressed.CompressedDData;
import org.eclipse.ditto.services.utils.pubsub.ddata.compressed.CompressedDDataHandler;
import org.eclipse.ditto.services.utils.pubsub.ddata.literal.LiteralDData;
import org.eclipse.ditto.services.utils.pubsub.ddata.literal.LiteralDDataHandler;
import org.eclipse.ditto.services.utils.pubsub.ddata.literal.LiteralUpdate;
import org.eclipse.ditto.services.utils.pubsub.extractors.PubSubTopicExtractor;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ActorRefFactory;
import akka.actor.ActorSystem;
import akka.actor.ExtendedActorSystem;
import akka.actor.Props;

/**
 * Creator of pub-sub access. Should not be instantiated more than once per instance.
 *
 * @param <T> type of messages.
 */
public abstract class AbstractPubSubFactory<T> implements PubSubFactory<T> {

    protected final ActorRefFactory actorRefFactory;
    protected final Class<T> messageClass;
    protected final String factoryId;
    protected final PubSubTopicExtractor<T> topicExtractor;

    protected final DistributedDataConfig ddataConfig;
    protected final DData<?, ?> ddata;
    @Nullable protected final DData<String, LiteralUpdate> acksDdata;

    /**
     * Create a pub-sub factory.
     *
     * @param context context of the actor under which publisher and subscriber actors are created.
     * @param messageClass the class of messages to publish and subscribe for.
     * @param topicExtractor a function extracting from each message the topics it was published at.
     * @param provider provider of the underlying ddata extension.
     * @param acksProvider provider of a second ddata extension for declared acknowledgement labels.
     */
    protected AbstractPubSubFactory(final ActorContext context,
            final Class<T> messageClass,
            final PubSubTopicExtractor<T> topicExtractor,
            final DDataProvider provider,
            @Nullable final LiteralDDataProvider acksProvider) {

        this.actorRefFactory = context;
        this.messageClass = messageClass;
        factoryId = provider.clusterRole;
        this.topicExtractor = topicExtractor;
        ddataConfig = provider.getConfig(context.system());
        ddata = CompressedDData.of(context.system(), provider);
        if (acksProvider != null) {
            acksDdata = LiteralDData.of(context.system(), acksProvider);
        } else {
            acksDdata = null;
        }
    }

    @Override
    public DistributedPub<T> startDistributedPub() {
        final String pubSupervisorName = factoryId + "-pub-supervisor";
        final Props pubSupervisorProps = PubSupervisor.props(ddata);
        final ActorRef pubSupervisor = actorRefFactory.actorOf(pubSupervisorProps, pubSupervisorName);
        return DistributedPub.of(pubSupervisor, topicExtractor);
    }

    @Override
    public DistributedSub startDistributedSub() {
        final String subSupervisorName = factoryId + "-sub-supervisor";
        final Props subSupervisorProps = SubSupervisor.props(messageClass, topicExtractor, ddata, acksDdata);
        final ActorRef subSupervisor = actorRefFactory.actorOf(subSupervisorProps, subSupervisorName);
        return DistributedSub.of(ddataConfig, subSupervisor);
    }

    /**
     * Default provider of factory-specific distributed data extensions.
     * Instances should be static variables so that they are not created more than once per JVM.
     */
    protected static final class DDataProvider extends CompressedDData.Provider {

        private final String clusterRole;

        private DDataProvider(final String clusterRole) {
            this.clusterRole = clusterRole;
        }

        /**
         * Create a distributed data provider.
         *
         * @param clusterRole Cluster role that uniquely identifies this provider.
         * @return the ddata provider.
         */
        public static DDataProvider of(final String clusterRole) {
            return new DDataProvider(clusterRole);
        }

        @Override
        public CompressedDDataHandler createExtension(final ExtendedActorSystem system) {
            return CompressedDDataHandler.create(system, getConfig(system), clusterRole, PubSubConfig.of(system));
        }

        @Override
        public DistributedDataConfig getConfig(final ActorSystem actorSystem) {
            return DistributedData.createConfig(actorSystem, clusterRole + "-replicator", clusterRole);
        }
    }

    /**
     * Literal DData provider.
     */
    protected static final class LiteralDDataProvider extends LiteralDData.Provider {

        private final String clusterRole;
        private final String messageType;

        private LiteralDDataProvider(final String clusterRole, final String messageType) {
            this.clusterRole = clusterRole;
            this.messageType = messageType;
        }

        /**
         * Create a distributed data provider.
         *
         * @param clusterRole Cluster role where this provider start.
         * @param messageType Message type that uniquely identifies this provider.
         * @return the ddata provider.
         */
        public static LiteralDDataProvider of(final String clusterRole, final String messageType) {
            return new LiteralDDataProvider(clusterRole, messageType);
        }

        @Override
        public LiteralDDataHandler createExtension(final ExtendedActorSystem system) {
            return LiteralDDataHandler.create(system, getConfig(system), messageType);
        }

        @Override
        public DistributedDataConfig getConfig(final ActorSystem actorSystem) {
            return DistributedData.createConfig(actorSystem, messageType + "-replicator", clusterRole);
        }
    }
}
