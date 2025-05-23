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

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacyCustomSharedRefEnforcementByProject;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacyDefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacySharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacySharedRefEnforcement.EnforcePolicy;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.OutOfSyncException;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.Policy;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.transport.ReceiveCommand;

/** Enables the detection of out-of-sync by validating batch ref updates against the global refdb */
public class BatchRefUpdateValidator extends RefUpdateValidator {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /** {@code BatchRefUpdateValidator} Factory for Guice assisted injection. */
  public interface Factory {
    BatchRefUpdateValidator create(
        String projectName, RefDatabase refDb, ImmutableSet<String> ignoredRefs);
  }

  public interface BatchValidationWrapper {
    void apply(BatchRefUpdate batchRefUpdate, NoParameterVoidFunction arg) throws IOException;
  }

  /**
   * Constructs a {@code BatchRefUpdateValidator} able to check the validity of batch ref-updates
   * against global refdb before execution.
   *
   * @param sharedRefDb an instance of the global refdb to check for out-of-sync refs.
   * @param validationMetrics to update validation results, such as split-brains.
   * @param refEnforcement Specific ref enforcements for this project.
   * @param legacyRefEnforcement Specific legacy ref enforcements for this project. Either a {@link
   *     LegacyCustomSharedRefEnforcementByProject} when custom policies are provided via
   *     configuration * file or a {@link LegacyDefaultSharedRefEnforcement} for defaults.
   * @param projectsFilter filter to match whether the project being updated should be validated
   *     against global refdb
   * @param projectName the name of the project being updated.
   * @param refDb for ref operations
   * @param ignoredRefs A set of refs for which updates should not be checked against the shared
   *     ref-db
   */
  @Inject
  public BatchRefUpdateValidator(
      SharedRefDatabaseWrapper sharedRefDb,
      ValidationMetrics validationMetrics,
      SharedRefEnforcement refEnforcement,
      LegacySharedRefEnforcement legacyRefEnforcement,
      ProjectsFilter projectsFilter,
      @Assisted String projectName,
      @Assisted RefDatabase refDb,
      @Assisted ImmutableSet<String> ignoredRefs) {
    super(
        sharedRefDb,
        validationMetrics,
        refEnforcement,
        legacyRefEnforcement,
        projectsFilter,
        projectName,
        refDb,
        ignoredRefs);
  }

  /**
   * Checks whether the provided batchRefUpdate should be validated first against the global refdb.
   * If not it just execute the provided batchRefUpdateFunction. Upon success the batchRefUpdate is
   * returned, upon failure split brain metrics are incremented and a {@link IOException} is thrown.
   *
   * <p>Validation is performed when either of these condition is true:
   *
   * <ul>
   *   <li>The project being updated is a global project ({@link
   *       RefUpdateValidator#isGlobalProject(String)}
   *   <li>The enforcement policy for the project being updated is {@link Policy#EXCLUDE}
   * </ul>
   *
   * @param batchRefUpdate batchRefUpdate object
   * @param batchRefUpdateFunction batchRefUpdate function to execute upon validation
   * @param batchRefUpdateRollbackFunction function to invoke when the ref-update needs to be rolled
   *     back
   * @throws IOException batch update failed
   */
  @SuppressWarnings("JavadocReference")
  public void executeBatchUpdateWithValidation(
      BatchRefUpdate batchRefUpdate,
      NoParameterVoidFunction batchRefUpdateFunction,
      OneParameterVoidFunction<List<ReceiveCommand>> batchRefUpdateRollbackFunction)
      throws IOException {
    if (refEnforcement.getPolicy(projectName) == Policy.EXCLUDE || !isGlobalProject(projectName)) {
      batchRefUpdateFunction.invoke();
      return;
    }
    if (legacyRefEnforcement.getPolicy(projectName) == EnforcePolicy.IGNORED
        || !isGlobalProject(projectName)) {
      batchRefUpdateFunction.invoke();
      return;
    }

    try {
      doExecuteBatchUpdate(batchRefUpdate, batchRefUpdateFunction, batchRefUpdateRollbackFunction);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log(
          "Failed to execute Batch Update on project %s", projectName);
      if (refEnforcement.getPolicy(projectName) == Policy.INCLUDE) {
        throw e;
      }
      if (legacyRefEnforcement.getPolicy(projectName) == EnforcePolicy.REQUIRED) {
        throw e;
      }
    }
  }

  private void doExecuteBatchUpdate(
      BatchRefUpdate batchRefUpdate,
      NoParameterVoidFunction delegateUpdate,
      OneParameterVoidFunction<List<ReceiveCommand>> delegateUpdateRollback)
      throws IOException {

    List<ReceiveCommand> commands = batchRefUpdate.getCommands();
    if (commands.isEmpty()) {
      return;
    }

    List<RefUpdateSnapshot> refsToUpdate =
        getRefUpdateSnapshots(commands).collect(Collectors.toList());
    List<RefUpdateSnapshot> refsFailures =
        refsToUpdate.stream().filter(RefUpdateSnapshot::hasFailed).collect(Collectors.toList());
    if (!refsFailures.isEmpty()) {
      String allFailuresMessage =
          refsFailures.stream()
              .map(refSnapshot -> String.format("Failed to fetch ref %s", refSnapshot.getName()))
              .collect(Collectors.joining(", "));
      Exception firstFailureException = refsFailures.get(0).getException();

      logger.atSevere().withCause(firstFailureException).log("%s", allFailuresMessage);
      throw new IOException(allFailuresMessage, firstFailureException);
    }

    try (CloseableSet<AutoCloseable> locks = new CloseableSet<>()) {
      final List<RefUpdateSnapshot> finalRefsToUpdate =
          compareAndGetLatestLocalRefs(refsToUpdate, locks);
      delegateUpdate.invoke();
      boolean sharedDbUpdateSucceeded = false;
      try {
        updateSharedRefDb(batchRefUpdate.getCommands().stream(), finalRefsToUpdate);
        sharedDbUpdateSucceeded = true;
      } catch (Exception e) {
        logger.atWarning().withCause(e).log(
            "Batch ref-update failed because of failure during the global refdb update.");
      } finally {
        if (!sharedDbUpdateSucceeded) {
          List<ReceiveCommand> receiveCommands = batchRefUpdate.getCommands();
          logger.atWarning().log(
              "Batch ref-update failed, set all commands Result to LOCK_FAILURE [%d]",
              commands.size());
          rollback(delegateUpdateRollback, finalRefsToUpdate, receiveCommands);
        }
      }
    } catch (OutOfSyncException e) {
      List<ReceiveCommand> receiveCommands = batchRefUpdate.getCommands();
      logger.atWarning().withCause(e).log(
          "Batch ref-update failing because node is out of sync with the shared ref-db. Set all"
              + " commands Result to LOCK_FAILURE [%d]",
          receiveCommands.size());
      receiveCommands.forEach((command) -> command.setResult(ReceiveCommand.Result.LOCK_FAILURE));
    }
  }

  private void rollback(
      OneParameterVoidFunction<List<ReceiveCommand>> delegateUpdateRollback,
      List<RefUpdateSnapshot> refsBeforeUpdate,
      List<ReceiveCommand> receiveCommands)
      throws IOException {
    List<ReceiveCommand> rollbackCommands =
        refsBeforeUpdate.stream()
            .map(
                refBeforeUpdate ->
                    new ReceiveCommand(
                        refBeforeUpdate.getNewValue(),
                        refBeforeUpdate.getOldValue(),
                        refBeforeUpdate.getName()))
            .collect(Collectors.toList());
    delegateUpdateRollback.invoke(rollbackCommands);
    receiveCommands.forEach(command -> command.setResult(ReceiveCommand.Result.LOCK_FAILURE));
  }

  private void updateSharedRefDb(
      Stream<ReceiveCommand> commandStream, List<RefUpdateSnapshot> refsToUpdate)
      throws IOException {
    if (commandStream.anyMatch(cmd -> cmd.getResult() != ReceiveCommand.Result.OK)) {
      return;
    }

    for (RefUpdateSnapshot refUpdateSnapshot : refsToUpdate) {
      updateSharedDbOrThrowExceptionFor(refUpdateSnapshot);
    }
  }

  private Stream<RefUpdateSnapshot> getRefUpdateSnapshots(List<ReceiveCommand> receivedCommands) {
    return receivedCommands.stream().map(this::getRefUpdateSnapshotForCommand);
  }

  private RefUpdateSnapshot getRefUpdateSnapshotForCommand(ReceiveCommand command) {
    try {
      switch (command.getType()) {
        case CREATE:
          return new RefUpdateSnapshot(nullRef(command.getRefName()), getNewValue(command));

        case UPDATE:
        case UPDATE_NONFASTFORWARD:
          return new RefUpdateSnapshot(getCurrentRef(command.getRefName()), getNewValue(command));

        case DELETE:
          return new RefUpdateSnapshot(getCurrentRef(command.getRefName()), ObjectId.zeroId());

        default:
          return new RefUpdateSnapshot(
              command.getRef(),
              new IllegalArgumentException("Unsupported command type " + command.getType()));
      }
    } catch (IOException e) {
      return new RefUpdateSnapshot(command.getRef(), e);
    }
  }

  private ObjectId getNewValue(ReceiveCommand command) {
    return command.getNewId();
  }

  private List<RefUpdateSnapshot> compareAndGetLatestLocalRefs(
      List<RefUpdateSnapshot> refsToUpdate, CloseableSet<AutoCloseable> locks) throws IOException {
    List<RefUpdateSnapshot> latestRefsToUpdate = new ArrayList<>();
    for (RefUpdateSnapshot refUpdateSnapshot : refsToUpdate) {
      latestRefsToUpdate.add(compareAndGetLatestLocalRef(refUpdateSnapshot, locks));
    }
    return latestRefsToUpdate;
  }

  private static final Ref nullRef(String refName) {
    return new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, refName, ObjectId.zeroId());
  }
}
