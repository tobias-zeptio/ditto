/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.signals.acks;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.model.base.acks.AcknowledgementLabel;
import org.eclipse.ditto.model.base.common.HttpStatusCode;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.signals.base.Signal;
import org.eclipse.ditto.signals.base.WithOptionalEntity;

/**
 * Acknowledgements aggregate several {@link Acknowledgement}s and contain an aggregated overall
 * {@link #getStatusCode() statusCode} describing the aggregated status of all contained Acknowledgements.
 * <p>
 * The {@link #getStatusCode()} is determined by the following algorithm:
 * <ul>
 * <li>When only one {@code Acknowledgement} is included: the {@link Acknowledgement#getStatusCode()} of this Ack is used as the {@code statusCode}</li>
 * <li>When several {@code Acknowledgement}s are included:
 * <ul>
 * <li>If all contained {@code Acknowledgement}s are successful, the overall {@link #getStatusCode()} is {@link HttpStatusCode#OK}</li>
 * <li>If at least one {@code Acknowledgement} is not, the overall {@link #getStatusCode()} is {@link HttpStatusCode#FAILED_DEPENDENCY}</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @since 1.1.0
 */
@Immutable
public interface Acknowledgements extends Iterable<Acknowledgement>, Signal<Acknowledgements>, WithOptionalEntity {

    /**
     * Type of the Acknowledgements.
     */
    String TYPE = "acknowledgements";

    /**
     * Returns a new {@code Acknowledgements} combining several passed in {@code Acknowledgement}s with a combined
     * {@code statusCode}.
     *
     * @param entityId the ID of the affected entity being acknowledged.
     * @param statusCode the aggregated status code (HTTP semantics) of the Acknowledgements.
     * @param acknowledgements the map of {@link Acknowledgement}s to be included in the aggregated Acknowledgements.
     * @param dittoHeaders the DittoHeaders of the Acknowledgements.
     * @return the Acknowledgements.
     * @throws NullPointerException if one of the required parameters was {@code null}.
     * @throws IllegalArgumentException if {@code entityId} is empty.
     */
    static Acknowledgements of(final CharSequence entityId,
            final HttpStatusCode statusCode,
            final Map<AcknowledgementLabel, Acknowledgement> acknowledgements,
            final DittoHeaders dittoHeaders) {

        return AcknowledgementFactory.newAcknowledgements(entityId, statusCode, acknowledgements, dittoHeaders);
    }

    /**
     * Returns the status code of the Acknowledgements.
     *
     * @return the status code of the Acknowledgements.
     */
    HttpStatusCode getStatusCode();

    /**
     * Returns a set containing the the AcknowledgementLabels.
     *
     * @return the unanswered acknowledgement labels.
     * Changes on the returned set are not reflected back to this AcknowledgementsPerRequest instance.
     */
    Set<AcknowledgementLabel> getMissingAcknowledgementLabels();

    /**
     * Returns a set containing the the successful acknowledgements.
     *
     * @return the successful acknowledgements.
     * The returned set maintains the order in which the acknowledgement were received.
     * Changes on the returned set are not reflected back to this AcknowledgementsPerRequest instance.
     */
    Set<Acknowledgement> getSuccessfulAcknowledgements();

    /**
     * Returns a set containing the the failed acknowledgements.
     *
     * @return the failed acknowledgements.
     * The returned set maintains the order in which the acknowledgement were received.
     * Changes on the returned set are not reflected back to this AcknowledgementsPerRequest instance.
     */
    Set<Acknowledgement> getFailedAcknowledgements();

    /**
     * Returns the size of the Acknowledgements, i. e. the number of contained values.
     *
     * @return the size.
     */
    int getSize();

    /**
     * Indicates whether this Acknowledgements is empty.
     *
     * @return {@code true} if this Acknowledgements does not contain any values, {@code false} else.
     */
    boolean isEmpty();

    /**
     * Returns a sequential {@code Stream} with the values of this Acknowledgements as its source.
     *
     * @return a sequential stream of the Acknowledgements of this container.
     */
    Stream<Acknowledgement> stream();

    /**
     * Returns all non hidden marked fields of this Acknowledgement.
     *
     * @return a JSON object representation of this Acknowledgement including only non hidden marked fields.
     */
    @Override
    default JsonObject toJson() {
        return toJson(FieldType.notHidden());
    }

    @Override
    default String getManifest() {
        return getType();
    }

    @Override
    default String getType() {
        return TYPE;
    }

    @Override
    default JsonPointer getResourcePath() {
        return JsonPointer.empty();
    }

    @Override
    default String getResourceType() {
        return getType();
    }


    /**
     * Definition of fields of the JSON representation of an {@link Acknowledgements}.
     */
    final class JsonFields {

        private JsonFields() {
            throw new AssertionError();
        }

        /**
         * The type of the Acknowledgements entity ID.
         */
        static final JsonFieldDefinition<String> ENTITY_ID =
                JsonFactory.newStringFieldDefinition("entityId", FieldType.REGULAR, JsonSchemaVersion.V_1,
                        JsonSchemaVersion.V_2);

        /**
         * The type of the Acknowledgements' statusCode.
         */
        static final JsonFieldDefinition<Integer> STATUS_CODE =
                JsonFactory.newIntFieldDefinition("statusCode", FieldType.REGULAR, JsonSchemaVersion.V_1,
                        JsonSchemaVersion.V_2);

        /**
         * The type of the Acknowledgements' acknowledgements.
         */
        static final JsonFieldDefinition<JsonObject> ACKNOWLEDGEMENTS =
                JsonFactory.newJsonObjectFieldDefinition("acknowledgements", FieldType.REGULAR, JsonSchemaVersion.V_1,
                        JsonSchemaVersion.V_2);

        /**
         * The type of the Acknowledgements DittoHeaders.
         */
        static final JsonFieldDefinition<JsonObject> DITTO_HEADERS =
                JsonFactory.newJsonObjectFieldDefinition("dittoHeaders", FieldType.REGULAR, JsonSchemaVersion.V_1,
                        JsonSchemaVersion.V_2);

    }
}
