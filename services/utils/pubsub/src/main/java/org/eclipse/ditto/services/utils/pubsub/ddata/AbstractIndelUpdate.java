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
package org.eclipse.ditto.services.utils.pubsub.ddata;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;

/**
 * A generic implementation of IndelUpdate.
 */
@NotThreadSafe
public abstract class AbstractIndelUpdate<S, T extends AbstractIndelUpdate<S, T>> implements IndelUpdate<S, T> {

    private Set<S> inserts;
    private Set<S> deletes;
    private boolean replaceAll;

    /**
     * Create an IndelUpdate.
     *
     * @param inserts what to insert.
     * @param deletes what to delete.
     * @param replaceAll whether it is a replacement update.
     */
    protected AbstractIndelUpdate(final Set<S> inserts, final Set<S> deletes, final boolean replaceAll) {
        this.inserts = inserts;
        this.deletes = deletes;
        this.replaceAll = replaceAll;
    }

    @Override
    public void reset() {
        inserts = new HashSet<>();
        deletes = new HashSet<>();
        replaceAll = false;
    }

    @Override
    public Set<S> getInserts() {
        return inserts;
    }

    @Override
    public Set<S> getDeletes() {
        return deletes;
    }

    @Override
    public boolean shouldReplaceAll() {
        return replaceAll;
    }

    @Override
    public void insert(final S newInserts) {
        inserts.add(newInserts);
        deletes.remove(newInserts);
    }

    @Override
    public void delete(final S newDeletes) {
        inserts.remove(newDeletes);
        deletes.add(newDeletes);
    }

    @Override
    public boolean equals(final Object other) {
        if (getClass().isInstance(other)) {
            final AbstractIndelUpdate<?, ?> that = getClass().cast(other);
            return replaceAll == that.replaceAll && inserts.equals(that.inserts) && deletes.equals(that.deletes);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(inserts, deletes, replaceAll);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                "inserts=" + inserts +
                ", deletes=" + deletes +
                ", replaceAll=" + replaceAll +
                "]";
    }
}
