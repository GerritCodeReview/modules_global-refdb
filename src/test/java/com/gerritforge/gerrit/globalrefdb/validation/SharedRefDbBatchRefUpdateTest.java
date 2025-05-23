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

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.DraftCommentEventsEnabledProvider;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.LegacyDefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceiveCommand.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SharedRefDbBatchRefUpdateTest implements RefFixture {

  @Mock SharedRefDatabaseWrapper sharedRefDb;
  @Mock BatchRefUpdate batchRefUpdate;
  @Mock BatchRefUpdateValidator batchRefUpdateValidator;
  @Mock RefDatabase refDatabase;
  @Mock RevWalk revWalk;
  @Mock ProgressMonitor progressMonitor;
  @Mock ValidationMetrics validationMetrics;
  @Mock ProjectsFilter projectsFilter;

  private final Ref oldRef =
      new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, A_TEST_REF_NAME, AN_OBJECT_ID_1);
  private final Ref newRef =
      new ObjectIdRef.Unpeeled(Ref.Storage.NETWORK, A_TEST_REF_NAME, AN_OBJECT_ID_2);
  ReceiveCommand receiveCommandBeforeExecution =
      createReceiveCommand(
          oldRef.getObjectId(), newRef.getObjectId(), oldRef.getName(), Result.NOT_ATTEMPTED);

  ReceiveCommand successReceiveCommandAfterExecution =
      createReceiveCommand(oldRef.getObjectId(), newRef.getObjectId(), oldRef.getName(), Result.OK);

  ReceiveCommand rejectReceiveCommandAfterExecution =
      createReceiveCommand(
          oldRef.getObjectId(),
          newRef.getObjectId(),
          oldRef.getName(),
          Result.REJECTED_NONFASTFORWARD);

  private ReceiveCommand createReceiveCommand(
      ObjectId oldRefObjectId, ObjectId newRefObjectId, String refName, Result result) {
    ReceiveCommand receiveCommand = new ReceiveCommand(oldRefObjectId, newRefObjectId, refName);
    receiveCommand.setResult(result);
    return receiveCommand;
  }

  private SharedRefDbBatchRefUpdate sharedRefDbRefUpdate;

  @Rule public TestName nameRule = new TestName();

  @Override
  public String testBranch() {
    return "branch_" + nameRule.getMethodName();
  }

  @Before
  public void setup() {
    when(projectsFilter.matches(anyString())).thenReturn(true);
  }

  private void setMockRequiredReturnValues() throws IOException {

    doReturn(batchRefUpdate).when(refDatabase).newBatchUpdate();

    when(batchRefUpdate.getCommands())
        .thenReturn(asList(receiveCommandBeforeExecution))
        .thenReturn(asList(successReceiveCommandAfterExecution));

    // Only needed to prevent NPEs should a rollback happen on the global-refdb updates
    // and not supposed to be used if all tests are successful.
    lenient().when(batchRefUpdate.addCommand(any(List.class))).thenReturn(batchRefUpdate);

    lenient().when(refDatabase.exactRef(A_TEST_REF_NAME)).thenReturn(oldRef).thenReturn(newRef);
    lenient().when(refDatabase.findRef(A_TEST_REF_NAME)).thenReturn(oldRef).thenReturn(newRef);

    sharedRefDbRefUpdate = getSharedRefDbBatchRefUpdateWithDefaultPolicyEnforcement();

    verifyNoInteractions(validationMetrics);
  }

  @Test
  public void executeAndDelegateSuccessfullyWithNoExceptions() throws Exception {
    setMockRequiredReturnValues();

    // When compareAndPut against sharedDb succeeds
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, oldRef);
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(eq(A_TEST_PROJECT_NAME_KEY), refEquals(oldRef), eq(newRef.getObjectId()));
    sharedRefDbRefUpdate.execute(revWalk, progressMonitor, Collections.emptyList());
    verify(sharedRefDb)
        .compareAndPut(eq(A_TEST_PROJECT_NAME_KEY), refEquals(oldRef), eq(newRef.getObjectId()));
  }

  private Ref refEquals(Ref oldRef) {
    return argThat(new RefMatcher(oldRef));
  }

  @Test(expected = IOException.class)
  public void executeAndFailsWithExceptions() throws IOException {
    sharedRefDbRefUpdate = getSharedRefDbBatchRefUpdateWithMockedValidator();
    doThrow(new IOException("IO Test Exception"))
        .when(batchRefUpdateValidator)
        .executeBatchUpdateWithValidation(any(), any(), any());

    sharedRefDbRefUpdate.execute(revWalk, progressMonitor, Collections.emptyList());
  }

  @Test
  public void executeSuccessfullyWithNoExceptionsWhenOutOfSync() throws IOException {
    setMockRequiredReturnValues();
    doReturn(true).when(sharedRefDb).exists(A_TEST_PROJECT_NAME_KEY, A_TEST_REF_NAME);
    doReturn(false).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, oldRef);

    sharedRefDbRefUpdate.execute(revWalk, progressMonitor, Collections.emptyList());

    verify(validationMetrics).incrementSplitBrainPrevention();
  }

  @Test
  public void executeSuccessfullyWithNoExceptionsWhenEmptyList() throws IOException {
    doReturn(batchRefUpdate).when(refDatabase).newBatchUpdate();
    doReturn(EMPTY_LIST).when(batchRefUpdate).getCommands();

    sharedRefDbRefUpdate = getSharedRefDbBatchRefUpdateWithDefaultPolicyEnforcement();

    sharedRefDbRefUpdate.execute(revWalk, progressMonitor, Collections.emptyList());
  }

  private SharedRefDbBatchRefUpdate getSharedRefDbBatchRefUpdateWithDefaultPolicyEnforcement() {
    BatchRefUpdateValidator.Factory batchRefValidatorFactory =
        new BatchRefUpdateValidator.Factory() {
          @Override
          public BatchRefUpdateValidator create(
              String projectName, RefDatabase refDb, ImmutableSet<String> ignoredRefs) {
            return new BatchRefUpdateValidator(
                sharedRefDb,
                validationMetrics,
                new SharedRefEnforcement(
                    new SharedRefDbConfiguration(new Config(), "testplugin"),
                    new DraftCommentEventsEnabledProvider(new Config())),
                new LegacyDefaultSharedRefEnforcement(),
                projectsFilter,
                projectName,
                refDb,
                ignoredRefs);
          }
        };
    return new SharedRefDbBatchRefUpdate(
        batchRefValidatorFactory, A_TEST_PROJECT_NAME, refDatabase, ImmutableSet.of());
  }

  private SharedRefDbBatchRefUpdate getSharedRefDbBatchRefUpdateWithMockedValidator() {
    BatchRefUpdateValidator.Factory batchRefValidatorFactory =
        (projectName, refDb, ignoredRefs) -> batchRefUpdateValidator;
    return new SharedRefDbBatchRefUpdate(
        batchRefValidatorFactory, A_TEST_PROJECT_NAME, refDatabase, ImmutableSet.of());
  }

  protected static class RefMatcher implements ArgumentMatcher<Ref> {
    private Ref left;

    public RefMatcher(Ref ref) {
      this.left = ref;
    }

    @Override
    public boolean matches(Ref right) {
      return left.getName().equals(right.getName())
          && left.getObjectId().equals(right.getObjectId());
    }
  }
}
