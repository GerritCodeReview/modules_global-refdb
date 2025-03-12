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

import com.gerritforge.gerrit.globalrefdb.ExtendedGlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.NoopSharedRefDatabase;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.metrics.Timer0.Context;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;

/**
 * Wraps an instance of {@link GlobalRefDatabase} provided as {@link DynamicItem} via a Guice
 * binding. Such instance is bound optionally and, in case no explicit binding is registered a
 * {@link NoopSharedRefDatabase} instance is wrapped instead.
 */
@Singleton
public class SharedRefDatabaseWrapper implements ExtendedGlobalRefDatabase {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final GlobalRefDatabase NOOP_REFDB = new NoopSharedRefDatabase();

  @Inject(optional = true)
  private DynamicItem<GlobalRefDatabase> sharedRefDbDynamicItem;

  private final SharedRefLogger sharedRefLogger;
  private final SharedRefDBMetrics metrics;

  /**
   * Constructs a {@code SharedRefDatabaseWrapper} wrapping an optional {@link GlobalRefDatabase},
   * which might have been bound by consumers of this library.
   *
   * @param sharedRefLogger logger of shared ref-db operations.
   */
  @Inject
  public SharedRefDatabaseWrapper(SharedRefLogger sharedRefLogger, SharedRefDBMetrics metrics) {
    this.sharedRefLogger = sharedRefLogger;
    this.metrics = metrics;
  }

  @VisibleForTesting
  public SharedRefDatabaseWrapper(
      DynamicItem<GlobalRefDatabase> sharedRefDbDynamicItem,
      SharedRefLogger sharedRefLogger,
      SharedRefDBMetrics metrics) {
    this(sharedRefLogger, metrics);
    this.sharedRefDbDynamicItem = sharedRefDbDynamicItem;
  }

  @Override
  public boolean isUpToDate(Project.NameKey project, Ref ref) throws GlobalRefDbLockException {
    return trackFailingOperation(
        () -> sharedRefDb().isUpToDate(project, ref), metrics::startIsUpToDateExecutionTime);
  }

  /** {@inheritDoc}. The operation is logged upon success. */
  @Override
  public boolean compareAndPut(Project.NameKey project, Ref currRef, ObjectId newRefValue)
      throws GlobalRefDbSystemError {

    return trackFailingOperation(
        () -> {
          boolean succeeded = sharedRefDb().compareAndPut(project, currRef, newRefValue);
          if (succeeded) {
            sharedRefLogger.logRefUpdate(project.get(), currRef, newRefValue);
          }
          return succeeded;
        },
        metrics::startCompareAndPutExecutionTime);
  }

  /** {@inheritDoc} the operation is logged upon success. */
  @Override
  public <T> boolean compareAndPut(Project.NameKey project, String refName, T currValue, T newValue)
      throws GlobalRefDbSystemError {

    return trackFailingOperation(
        () -> {
          boolean succeeded = sharedRefDb().compareAndPut(project, refName, currValue, newValue);
          if (succeeded) {
            sharedRefLogger.logRefUpdate(project.get(), refName, currValue, newValue);
          }
          return succeeded;
        },
        metrics::startCompareAndPutExecutionTime);
  }

  @Override
  public <T> void put(Project.NameKey project, String refName, T newValue)
      throws GlobalRefDbSystemError {
    if (!isSetOperationSupported()) {
      throw new UnsupportedOperationException(
          "GlobalRefDb implementation doesn't support set operation");
    }

    trackFailingOperation(
        () -> {
          ((ExtendedGlobalRefDatabase) sharedRefDb()).put(project, refName, newValue);
          sharedRefLogger.logRefUpdate(project.get(), refName, newValue);
          return null;
        },
        metrics::startSetExecutionTime);
  }

  public boolean isSetOperationSupported() {
    return sharedRefDb() instanceof ExtendedGlobalRefDatabase;
  }

  /** {@inheritDoc}. The operation is logged. */
  @Override
  public AutoCloseable lockRef(Project.NameKey project, String refName)
      throws GlobalRefDbLockException {
    return trackFailingOperation(
        () -> {
          AutoCloseable locker = sharedRefDb().lockRef(project, refName);
          sharedRefLogger.logLockAcquisition(project.get(), refName);
          return locker;
        },
        metrics::startLockRefExecutionTime);
  }

  @Override
  public boolean exists(Project.NameKey project, String refName) {
    return trackFailingOperation(
        () -> sharedRefDb().exists(project, refName), metrics::startExistsExecutionTime);
  }

  /** {@inheritDoc}. The operation is logged. */
  @Override
  public void remove(Project.NameKey project) throws GlobalRefDbSystemError {
    trackFailingOperation(
        () -> {
          sharedRefDb().remove(project);
          sharedRefLogger.logProjectDelete(project.get());
          return null;
        },
        metrics::startRemoveExecutionTime);
  }

  @Override
  public <T> Optional<T> get(Project.NameKey nameKey, String s, Class<T> clazz)
      throws GlobalRefDbSystemError {
    return trackFailingOperation(
        () -> sharedRefDb().get(nameKey, s, clazz), metrics::startGetExecutionTime);
  }

  boolean isNoop() {
    return sharedRefDbDynamicItem == null || sharedRefDbDynamicItem.get() == null;
  }

  private GlobalRefDatabase sharedRefDb() {
    if (sharedRefDbDynamicItem == null) {
      log.atWarning().log("DynamicItem<GlobalRefDatabase> has not been injected");
    }

    return Optional.ofNullable(sharedRefDbDynamicItem)
        .map(di -> di.get())
        .orElseGet(
            () -> {
              log.atWarning().log("Using NOOP_REFDB");
              return NOOP_REFDB;
            });
  }

  @FunctionalInterface
  private interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  private <T, E extends Exception> T trackFailingOperation(
      ThrowingSupplier<T, E> operation, Supplier<Context> metricTimer) throws E {

    boolean completedWithoutExceptions = false;
    try (Context ignore = metricTimer.get()) {
      T result = operation.get();
      completedWithoutExceptions = true;
      return result;
    } finally {
      if (!completedWithoutExceptions) {
        String callStack =
            Arrays.stream(Thread.currentThread().getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n   at "));
        log.atWarning().log("Global-refdb operation failed\n%s", callStack);
        metrics.incrementOperationFailures();
      }
    }
  }
}
