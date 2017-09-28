/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.signals.events.policies;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonField;
import org.eclipse.ditto.json.JsonFieldDefinition;
import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.json.JsonObjectBuilder;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.eclipse.ditto.model.policies.Label;
import org.eclipse.ditto.model.policies.PoliciesModelFactory;
import org.eclipse.ditto.model.policies.Resource;
import org.eclipse.ditto.model.policies.ResourceKey;
import org.eclipse.ditto.signals.events.base.EventJsonDeserializer;

/**
 * This event is emitted after a {@link Resource} was created.
 */
@Immutable
public final class ResourceCreated extends AbstractPolicyEvent<ResourceCreated>
        implements PolicyEvent<ResourceCreated> {

    /**
     * Name of this event.
     */
    public static final String NAME = "resourceCreated";

    /**
     * Type of this event.
     */
    public static final String TYPE = TYPE_PREFIX + NAME;

    static final JsonFieldDefinition JSON_LABEL =
            JsonFactory.newFieldDefinition("label", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_RESOURCE_KEY =
            JsonFactory.newFieldDefinition("resourceKey", String.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_2);

    static final JsonFieldDefinition JSON_RESOURCE =
            JsonFactory.newFieldDefinition("resource", JsonObject.class, FieldType.REGULAR,
                    // available in schema versions:
                    JsonSchemaVersion.V_2);

    private final Label label;
    private final Resource resource;

    private ResourceCreated(final String policyId, final Label label, final Resource resource, final long revision,
            final Instant timestamp, final DittoHeaders dittoHeaders) {
        super(TYPE, checkNotNull(policyId, "Policy identifier"), revision, timestamp, dittoHeaders);
        this.label = checkNotNull(label, "Label");
        this.resource = checkNotNull(resource, "Resource");
    }

    /**
     * Constructs a new {@code ResourceCreated} object.
     *
     * @param policyId the identifier of the Policy to which the created Resource belongs
     * @param label the label of the Policy Entry to which the created Resource belongs
     * @param resource the created {@link Resource}
     * @param revision the revision of the Policy.
     * @param dittoHeaders the headers of the command which was the cause of this event.
     * @return the created ResourceCreated.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public static ResourceCreated of(final String policyId, final Label label, final Resource resource,
            final long revision, final DittoHeaders dittoHeaders) {
        return of(policyId, label, resource, revision, null, dittoHeaders);
    }

    /**
     * Constructs a new {@code ResourceCreated} object.
     *
     * @param policyId the identifier of the Policy to which the created Resource belongs
     * @param label the label of the Policy Entry to which the created Resource belongs
     * @param resource the created {@link Resource}
     * @param revision the revision of the Policy.
     * @param timestamp the timestamp of this event.
     * @param dittoHeaders the headers of the command which was the cause of this event.
     * @return the created ResourceCreated.
     * @throws NullPointerException if any argument is {@code null}.
     */
    public static ResourceCreated of(final String policyId, final Label label, final Resource resource,
            final long revision, final Instant timestamp, final DittoHeaders dittoHeaders) {
        return new ResourceCreated(policyId, label, resource, revision, timestamp, dittoHeaders);
    }

    /**
     * Creates a new {@code ResourceCreated} from a JSON string.
     *
     * @param jsonString the JSON string from which a new ResourceCreated instance is to be created.
     * @param dittoHeaders the headers of the command which was the cause of this event.
     * @return the {@code ResourceCreated} which was created from the given JSON string.
     * @throws NullPointerException if {@code jsonString} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonString} was not in the expected 'ResourceCreated' format.
     */
    public static ResourceCreated fromJson(final String jsonString, final DittoHeaders dittoHeaders) {
        return fromJson(JsonFactory.newObject(jsonString), dittoHeaders);
    }

    /**
     * Creates a new {@code ResourceCreated} from a JSON object.
     *
     * @param jsonObject the JSON object from which a new ResourceCreated instance is to be created.
     * @param dittoHeaders the headers of the command which was the cause of this event.
     * @return the {@code ResourceCreated} which was created from the given JSON object.
     * @throws NullPointerException if {@code jsonObject} is {@code null}.
     * @throws org.eclipse.ditto.json.JsonParseException if the passed in {@code jsonObject} was not in the expected 'ResourceCreated' format.
     */
    public static ResourceCreated fromJson(final JsonObject jsonObject, final DittoHeaders dittoHeaders) {
        return new EventJsonDeserializer<ResourceCreated>(TYPE, jsonObject).deserialize((revision, timestamp,
                jsonObjectReader) -> {
            final String policyId = jsonObjectReader.get(JsonFields.POLICY_ID);
            final Label label = Label.of(jsonObjectReader.get(JSON_LABEL));
            final String resourceKey = jsonObjectReader.get(JSON_RESOURCE_KEY);
            final JsonObject resourceJsonObject = jsonObjectReader.get(JSON_RESOURCE);
            final Resource extractedCreatedResource =
                    PoliciesModelFactory.newResource(ResourceKey.newInstance(resourceKey),
                            resourceJsonObject);

            return of(policyId, label, extractedCreatedResource, revision, timestamp, dittoHeaders);
        });
    }

    /**
     * Returns the label of the Policy Entry to which the created Resource belongs.
     *
     * @return the label
     */
    public Label getLabel() {
        return label;
    }

    /**
     * Returns the created {@link Resource}.
     *
     * @return the created {@link Resource}.
     */
    public Resource getResource() {
        return resource;
    }

    @Override
    public Optional<JsonValue> getEntity(final JsonSchemaVersion schemaVersion) {
        return Optional.ofNullable(resource.toJson(schemaVersion, FieldType.regularOrSpecial()));
    }

    @Override
    public JsonPointer getResourcePath() {
        final String path = "/entries/" + label + "/resources" + resource.getPath();
        return JsonPointer.of(path);
    }

    @Override
    public ResourceCreated setRevision(final long revision) {
        return of(getPolicyId(), label, resource, revision, getTimestamp().orElse(null), getDittoHeaders());
    }

    @Override
    public ResourceCreated setDittoHeaders(final DittoHeaders dittoHeaders) {
        return of(getPolicyId(), label, resource, getRevision(), getTimestamp().orElse(null), dittoHeaders);
    }

    @Override
    protected void appendPayload(final JsonObjectBuilder jsonObjectBuilder, final JsonSchemaVersion schemaVersion,
            final Predicate<JsonField> thePredicate) {
        final Predicate<JsonField> predicate = schemaVersion.and(thePredicate);
        jsonObjectBuilder.set(JSON_LABEL, label.toString(), predicate);
        jsonObjectBuilder.set(JSON_RESOURCE_KEY, resource.getFullQualifiedPath(), predicate);
        jsonObjectBuilder.set(JSON_RESOURCE, resource.toJson(schemaVersion, thePredicate), predicate);
    }

    @SuppressWarnings("squid:S109")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hashCode(label);
        result = prime * result + Objects.hashCode(resource);
        return result;
    }

    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (null == o || getClass() != o.getClass()) {
            return false;
        }
        final ResourceCreated that = (ResourceCreated) o;
        return that.canEqual(this) && Objects.equals(label, that.label) && Objects.equals(resource, that.resource)
                && super.equals(that);
    }

    @Override
    protected boolean canEqual(final Object other) {
        return (other instanceof ResourceCreated);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + super.toString() + ", label=" + label + ", resource=" + resource +
                "]";
    }

}
