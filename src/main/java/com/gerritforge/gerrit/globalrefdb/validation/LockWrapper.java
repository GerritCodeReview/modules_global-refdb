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

/** Wrapper around an {@link AutoCloseable} lock to allow logging of resource releasing. */
public class LockWrapper implements AutoCloseable {
  private final String project;
  private final String refName;
  private final AutoCloseable lock;
  private final SharedRefLogger sharedRefLogger;
  private final SharedRefLogger.Scope scope;

  /**
   * Constructs a {@code LockWrapper} object for a specific refName of a project, which wraps a held
   * lock, so that its details can be logged upon closure.
   *
   * @param sharedRefLogger to log the releasing of the lock
   * @param project the project the lock has been acquired for
   * @param refName the refName the lock has been acquired for
   * @param lock the acquired lock
   */
  public LockWrapper(
      SharedRefLogger sharedRefLogger,
      String project,
      String refName,
      AutoCloseable lock,
      SharedRefLogger.Scope scope) {
    this.lock = lock;
    this.sharedRefLogger = sharedRefLogger;
    this.project = project;
    this.refName = refName;
    this.scope = scope;
    sharedRefLogger.logLockAcquisition(project, refName, scope);
  }

  /**
   * Release the acquired lock and logs the lock release operation.
   *
   * @throws Exception if this resource cannot be closed
   */
  @Override
  public void close() throws Exception {
    lock.close();
    sharedRefLogger.logLockRelease(project, refName, scope);
  }
}
