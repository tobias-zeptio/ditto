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
package org.eclipse.ditto.services.utils.pubsub.actors;

import java.util.Collection;

import org.eclipse.ditto.services.utils.metrics.DittoMetrics;
import org.eclipse.ditto.services.utils.metrics.instruments.counter.Counter;
import org.eclipse.ditto.services.utils.pubsub.ddata.SubscriptionsReader;
import org.eclipse.ditto.services.utils.pubsub.extractors.PubSubTopicExtractor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;

/**
 * Actor that distributes messages to local subscribers
 *
 * @param <T> type of messages.
 */
public final class Subscriber<T> extends AbstractActor {

    /**
     * Prefix of this actor's name.
     */
    public static final String ACTOR_NAME_PREFIX = "subscriber";

    private final Class<T> messageClass;
    private final PubSubTopicExtractor<T> topicExtractor;

    private SubscriptionsReader localSubscriptions = SubscriptionsReader.empty();
    private final Counter truePositiveCounter = DittoMetrics.counter("pubsub-true-positive");
    private final Counter falsePositiveCounter = DittoMetrics.counter("pubsub-false-positive");

    @SuppressWarnings("unused")
    private Subscriber(final Class<T> messageClass, final PubSubTopicExtractor<T> topicExtractor) {
        this.messageClass = messageClass;
        this.topicExtractor = topicExtractor;
    }

    /**
     * Create Props object for this actor.
     *
     * @param messageClass class of message distributed by the pub-sub.
     * @param topicExtractor extractor of topics from messages.
     * @param <T> type of messages.
     * @return the Props object.
     */
    public static <T> Props props(final Class<T> messageClass, final PubSubTopicExtractor<T> topicExtractor) {
        return Props.create(Subscriber.class, messageClass, topicExtractor);
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(messageClass, this::broadcastToLocalSubscribers)
                .match(SubscriptionsReader.class, this::updateLocalSubscriptions)
                .build();
    }

    private void broadcastToLocalSubscribers(final T message) {
        final Collection<String> topics = topicExtractor.getTopics(message);
        final Collection<ActorRef> localSubscribers = localSubscriptions.getSubscribers(topics);
        if (localSubscribers.isEmpty()) {
            falsePositiveCounter.increment();
        } else {
            truePositiveCounter.increment();
            for (final ActorRef localSubscriber : localSubscribers) {
                localSubscriber.tell(message, getSender());
            }
        }
    }

    private void updateLocalSubscriptions(final SubscriptionsReader localSubscriptions) {
        this.localSubscriptions = localSubscriptions;
    }

}
