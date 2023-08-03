// Copyright (C) 2020 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.gerritforge.gerrit.globalrefdb.validation;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

/**
 * A convenience object encompassing the old (current) and the new (candidate) value of a {@link
 * Ref}. This is used to snapshot the current status of a ref update so that validations against the
 * global refdb are unaffected by changes on the underlying {@link
 * org.eclipse.jgit.lib.RefDatabase}.
 */
class RefUpdateSnapshot {
  private final Ref ref;
  private final ObjectId newValue;
  private final Exception exception;

  /**
   * Constructs a {@code RefUpdateSnapshot} with the provided old and new values. The old value of
   * this Ref must not be null, otherwise an {@link IllegalArgumentException} is thrown.
   *
   * @param ref the ref with its old value
   * @param newRefValue the new (candidate) value for this ref.
   */
  RefUpdateSnapshot(Ref ref, ObjectId newRefValue) {
    if (ref == null) {
      throw new IllegalArgumentException(
          String.format("RefUpdateSnapshot cannot be created for null Ref"));
    }
    this.ref = ref;
    this.newValue = newRefValue;
    this.exception = null;
  }

  /**
   * Constructs a {@code RefUpdateSnapshot} with the current ref and an Exception indicating why the
   * ref's new value could not be retrieved.
   *
   * @param ref the ref with its old value
   * @param e exception caught when trying to retrieve the ref's new value
   */
  RefUpdateSnapshot(Ref ref, Exception e) {
    this.ref = ref;
    this.newValue = ObjectId.zeroId();
    this.exception = e;
  }

  /**
   * Get the ref name
   *
   * @return the ref name
   */
  public String getName() {
    return ref.getName();
  }

  /**
   * Get the ref's old value
   *
   * @return the ref's old value
   */
  public ObjectId getOldValue() {
    return ref.getObjectId();
  }

  /**
   * Get the ref's new (candidate) value
   *
   * @return the ref's new (candidate) value
   */
  public ObjectId getNewValue() {
    return newValue;
  }

  /**
   * Get the snapshotted ref with its old value
   *
   * @return the snapshotted ref with its old value
   */
  public Ref getRef() {
    return ref;
  }

  /**
   * Get the exception which occurred when retrieving the ref's new value
   *
   * @return the exception which occurred when retrieving the ref's new value
   */
  public Exception getException() {
    return exception;
  }

  /**
   * Whether retrieving the new (candidate) value failed
   *
   * @return {@code true} when retrieving the ref's new (candidate) value failed, {@code false}
   *     otherwise.
   */
  public boolean hasFailed() {
    return exception != null;
  }
}
