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

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.RefDbLockException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacyCustomSharedRefEnforcementByProject;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacyDefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacySharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacySharedRefEnforcement.EnforcePolicy;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.OutOfSyncException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedDbSplitBrainException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.Policy;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;

/** Enables the detection of out-of-sync by validating ref updates against the global refdb. */
public class RefUpdateValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  protected final SharedRefDatabaseWrapper sharedRefDb;
  protected final ValidationMetrics validationMetrics;

  protected final String projectName;
  protected final RefDatabase refDb;
  protected final SharedRefEnforcement refEnforcement;
  protected final LegacySharedRefEnforcement legacyRefEnforcement;
  protected final ProjectsFilter projectsFilter;
  private final ImmutableSet<String> ignoredRefs;

  /** {@code RefUpdateValidator} Factory for Guice assisted injection. */
  public interface Factory {
    RefUpdateValidator create(
        String projectName, RefDatabase refDb, ImmutableSet<String> ignoredRefs);
  }

  public interface ExceptionThrowingSupplier<T, E extends Exception> {
    T create() throws E;
  }

  public interface RefValidationWrapper {
    RefUpdate.Result apply(NoParameterFunction<RefUpdate.Result> arg, RefUpdate refUpdate)
        throws IOException;
  }

  public interface NoParameterFunction<T> {
    T invoke() throws IOException;
  }

  public interface NoParameterVoidFunction {
    void invoke() throws IOException;
  }

  public interface OneParameterFunction<F, T> {
    T invoke(F f) throws IOException;
  }

  public interface OneParameterVoidFunction<T> {
    void invoke(T f) throws IOException;
  }

  /**
   * Constructs a {@code RefUpdateValidator} able to check the validity of ref-updates against a
   * global refdb before execution.
   *
   * @param sharedRefDb an instance of the global refdb to check for out-of-sync refs.
   * @param validationMetrics to update validation results, such as split-brains.
   * @param refEnforcement Whether or not a given ref should be stored
   * @param legacyRefEnforcement Specific ref enforcements for this project. Either a {@link
   *     LegacyCustomSharedRefEnforcementByProject} when custom policies are provided via
   *     configuration file or a {@link LegacyDefaultSharedRefEnforcement} for defaults.
   * @param projectsFilter filter to match whether the project being updated should be validated
   *     against global refdb
   * @param projectName the name of the project being updated.
   * @param refDb for ref operations
   * @param ignoredRefs A set of refs for which updates should not be checked against the shared
   *     ref-db
   */
  @Inject
  public RefUpdateValidator(
      SharedRefDatabaseWrapper sharedRefDb,
      ValidationMetrics validationMetrics,
      SharedRefEnforcement refEnforcement,
      LegacySharedRefEnforcement legacyRefEnforcement,
      ProjectsFilter projectsFilter,
      @Assisted String projectName,
      @Assisted RefDatabase refDb,
      @Assisted ImmutableSet<String> ignoredRefs) {
    this.sharedRefDb = sharedRefDb;
    this.validationMetrics = validationMetrics;
    this.refDb = refDb;
    this.ignoredRefs = ignoredRefs;
    this.projectName = projectName;
    this.refEnforcement = refEnforcement;
    this.legacyRefEnforcement = legacyRefEnforcement;
    this.projectsFilter = projectsFilter;
  }

  /**
   * Checks whether the provided refUpdate should be validated first against the shared ref-db. If
   * not it just execute the provided refUpdateFunction. If it should be validated against the
   * global refdb then it does so by executing the {@link RefUpdateValidator#doExecuteRefUpdate}
   * first. Upon success the refUpdate is returned, upon failure split brain metrics are incremented
   * and a {@link SharedDbSplitBrainException} is thrown.
   *
   * <p>Validation is performed when either of these condition is true
   *
   * <ul>
   *   <li>The ref being updated is not to be ignored ({@link
   *       RefUpdateValidator#isRefToBeIgnored(String)})
   *   <li>The project being updated is a global project ({@link
   *       RefUpdateValidator#isGlobalProject(String)}
   *   <li>The enforcement policy for the project being updated is {@link EnforcePolicy#IGNORED}
   * </ul>
   *
   * @param refUpdate the refUpdate command
   * @param refUpdateFunction the refUpdate function to execute after validation
   * @param rollbackFunction function to invoke when the ref-update needs to be rolled back
   * @return the result of the update, or "null" in case a split brain was detected but the policy
   *     enforcement was not INCLUDE
   * @throws IOException Execution of ref update failed
   */
  public RefUpdate.Result executeRefUpdate(
      RefUpdate refUpdate,
      NoParameterFunction<RefUpdate.Result> refUpdateFunction,
      OneParameterFunction<ObjectId, Result> rollbackFunction)
      throws IOException {
    if (isRefToBeIgnored(refUpdate.getName())
        || !isGlobalProject(projectName)
        || refEnforcement.getPolicy(projectName) == Policy.EXCLUDE
        || legacyRefEnforcement.getPolicy(projectName) == EnforcePolicy.IGNORED) {
      return refUpdateFunction.invoke();
    }

    return doExecuteRefUpdate(refUpdate, refUpdateFunction, rollbackFunction);
  }

  private Boolean isRefToBeIgnored(String refName) {
    Boolean isRefToBeIgnored =
        ignoredRefs.stream().anyMatch(ignoredRefPrefix -> refName.startsWith(ignoredRefPrefix));
    logger.atFine().log("Is project version update? %b", isRefToBeIgnored);
    return isRefToBeIgnored;
  }

  private <T extends Throwable> void softFailBasedOnEnforcement(T e, Policy policy) throws T {
    logger.atWarning().withCause(e).log(
        "Failure while running with policy enforcement %s. Error message: %s",
        policy, e.getMessage());
    if (policy == Policy.INCLUDE) {
      throw e;
    }
  }

  protected Boolean isGlobalProject(String projectName) {
    Boolean isGlobalProject = projectsFilter.matches(projectName);
    logger.atFine().log("Is global project? %b", isGlobalProject);
    return isGlobalProject;
  }

  protected RefUpdate.Result doExecuteRefUpdate(
      RefUpdate refUpdate,
      NoParameterFunction<Result> refUpdateFunction,
      OneParameterFunction<ObjectId, Result> rollbackFunction)
      throws IOException {
    try (CloseableSet<AutoCloseable> locks = new CloseableSet<>()) {
      RefUpdateSnapshot refUpdateSnapshot = newSnapshot(refUpdate);
      compareAndGetLatestLocalRef(refUpdateSnapshot, locks);
      RefUpdate.Result result = refUpdateFunction.invoke();
      if (!isSuccessful(result)) {
        return result;
      }
      try {
        updateSharedDbOrThrowExceptionFor(refUpdateSnapshot);
      } catch (Exception e) {
        result = rollbackFunction.invoke(refUpdateSnapshot.getOldValue());
        if (isSuccessful(result)) {
          result = RefUpdate.Result.LOCK_FAILURE;
        }
        logger.atSevere().withCause(e).log(
            "Failed to update global refdb, the local refdb has been rolled back: %s",
            e.getMessage());
      }
      return result;
    } catch (RefDbLockException e) {
      logger.atWarning().withCause(e).log("Unable to lock %s:%s", projectName, refUpdate.getName());
      return Result.LOCK_FAILURE;
    } catch (OutOfSyncException e) {
      logger.atWarning().withCause(e).log(
          "Local node is out of sync with ref-db: %s", e.getMessage());
      return RefUpdate.Result.LOCK_FAILURE;
    }
  }

  protected void updateSharedDbOrThrowExceptionFor(RefUpdateSnapshot refSnapshot)
      throws IOException {
    // We are not checking refs that should be ignored
    final Policy refEnforcementPolicy =
        refEnforcement.getPolicy(projectName, refSnapshot.getName());
    final EnforcePolicy legacyRefEnforcementPolicy =
        legacyRefEnforcement.getPolicy(projectName, refSnapshot.getName());
    if (refEnforcementPolicy == Policy.EXCLUDE
        || legacyRefEnforcementPolicy == EnforcePolicy.IGNORED) {
      return;
    }

    boolean succeeded;
    try {
      if (!sharedRefDb.isNoop()) {
        ObjectId localObjectId =
            Optional.ofNullable(refDb.findRef(refSnapshot.getName()))
                .map(Ref::getObjectId)
                .orElse(ObjectId.zeroId());
        if (!localObjectId.equals(refSnapshot.getNewValue())) {
          String error =
              String.format(
                  "Aborting the global-refdb update of %s = %s: local ref value is %s instead of"
                      + " the expected value %s",
                  refSnapshot.getName(),
                  refSnapshot.getNewValue(),
                  localObjectId.name(),
                  refSnapshot.getNewValue());
          logger.atSevere().log("%s", error);
          throw new IOException(error);
        }
      }

      succeeded =
          sharedRefDb.compareAndPut(
              Project.nameKey(projectName), refSnapshot.getRef(), refSnapshot.getNewValue());
    } catch (GlobalRefDbSystemError e) {
      logger.atWarning().withCause(e).log(
          "Not able to persist the data in global-refdb for project '%s', ref '%s' and value %s,"
              + " message: %s",
          projectName, refSnapshot.getName(), refSnapshot.getNewValue(), e.getMessage());
      throw e;
    }

    if (!succeeded) {
      String errorMessage =
          String.format(
              "Not able to persist the data in SharedRef for project '%s' and ref '%s',"
                  + "the cluster is now in Split Brain since the commit has been "
                  + "persisted locally but not in global-refdb the value %s",
              projectName, refSnapshot.getName(), refSnapshot.getNewValue());
      throw new SharedDbSplitBrainException(errorMessage);
    }
  }

  protected RefUpdateSnapshot compareAndGetLatestLocalRef(
      RefUpdateSnapshot refUpdateSnapshot, CloseableSet<AutoCloseable> locks)
      throws GlobalRefDbLockException, OutOfSyncException, IOException {
    String refName = refUpdateSnapshot.getName();
    Policy refEnforcementPolicy = refEnforcement.getPolicy(projectName, refName);
    if (refEnforcementPolicy == Policy.EXCLUDE) {
      return refUpdateSnapshot;
    }

    EnforcePolicy legacyRefEnforcementPolicy = legacyRefEnforcement.getPolicy(projectName, refName);
    if (legacyRefEnforcementPolicy == EnforcePolicy.IGNORED) {
      return refUpdateSnapshot;
    }

    String sharedLockKey = String.format("%s:%s", projectName, refName);
    String localLockKey = String.format("%s:local", sharedLockKey);
    Project.NameKey projectKey = Project.nameKey(projectName);
    locks.addResourceIfNotExist(localLockKey, () -> sharedRefDb.lockLocalRef(projectKey, refName));
    locks.addResourceIfNotExist(sharedLockKey, () -> sharedRefDb.lockRef(projectKey, refName));

    RefUpdateSnapshot latestRefUpdateSnapshot = getLatestLocalRef(refUpdateSnapshot);
    if (sharedRefDb.isUpToDate(projectKey, latestRefUpdateSnapshot.getRef())) {
      return latestRefUpdateSnapshot;
    }

    if (isNullRef(latestRefUpdateSnapshot.getRef()) || sharedRefDb.exists(projectKey, refName)) {
      validationMetrics.incrementSplitBrainPrevention();

      softFailBasedOnEnforcement(
          new OutOfSyncException(projectName, latestRefUpdateSnapshot.getRef()),
          refEnforcementPolicy);
    }

    return latestRefUpdateSnapshot;
  }

  private boolean isNullRef(Ref ref) {
    return ref.getObjectId().equals(ObjectId.zeroId());
  }

  private RefUpdateSnapshot getLatestLocalRef(RefUpdateSnapshot refUpdateSnapshot)
      throws IOException {
    Ref latestRef = refDb.exactRef(refUpdateSnapshot.getName());
    return new RefUpdateSnapshot(
        latestRef == null ? nullRef(refUpdateSnapshot.getName()) : latestRef,
        refUpdateSnapshot.getNewValue());
  }

  private Ref nullRef(String name) {
    return new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, name, ObjectId.zeroId());
  }

  protected boolean isSuccessful(RefUpdate.Result result) {
    switch (result) {
      case NEW:
      case FORCED:
      case FAST_FORWARD:
      case NO_CHANGE:
      case RENAMED:
        return true;

      case REJECTED_OTHER_REASON:
      case REJECTED_MISSING_OBJECT:
      case REJECTED_CURRENT_BRANCH:
      case NOT_ATTEMPTED:
      case LOCK_FAILURE:
      case IO_FAILURE:
      case REJECTED:
      default:
        return false;
    }
  }

  protected RefUpdateSnapshot newSnapshot(RefUpdate refUpdate) throws IOException {
    return new RefUpdateSnapshot(getCurrentRef(refUpdate.getName()), refUpdate.getNewObjectId());
  }

  protected Ref getCurrentRef(String refName) throws IOException {
    return MoreObjects.firstNonNull(refDb.findRef(refName), nullRef(refName));
  }

  public static class CloseableSet<T extends AutoCloseable> implements AutoCloseable {
    private final HashMap<String, AutoCloseable> elements;

    public CloseableSet() {
      this(new HashMap<>());
    }

    public CloseableSet(HashMap<String, AutoCloseable> elements) {
      this.elements = elements;
    }

    public void addResourceIfNotExist(
        String key, ExceptionThrowingSupplier<T, RefDbLockException> resourceFactory)
        throws RefDbLockException {
      if (!elements.containsKey(key)) {
        elements.put(key, resourceFactory.create());
      }
    }

    @Override
    public void close() {
      elements.values().stream()
          .forEach(
              closeable -> {
                try {
                  closeable.close();
                } catch (Exception closingException) {
                  logger.atSevere().withCause(closingException).log(
                      "Exception trying to release resource %s, "
                          + "the locked resources won't be accessible in all cluster unless"
                          + " the lock is removed from global-refdb manually",
                      closeable);
                }
              });
    }
  }
}
