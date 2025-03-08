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

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static java.util.Collections.singletonList;
import static org.eclipse.jgit.transport.ReceiveCommand.Type.CREATE;
import static org.eclipse.jgit.transport.ReceiveCommand.Type.UPDATE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.RefUpdateValidator.OneParameterVoidFunction;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.metrics.DisabledMetricMaker;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.internal.storage.file.RefDirectory;
import org.eclipse.jgit.junit.LocalDiskRepositoryTestCase;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.BatchRefUpdate;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.eclipse.jgit.transport.ReceiveCommand.Result;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BatchRefUpdateValidatorTest extends LocalDiskRepositoryTestCase implements RefFixture {
  private static final String A_REF_NAME_1 = "refs/heads/a-ref-name-1";
  private static final String A_REF_NAME_2 = "refs/heads/a-ref-name-2";

  @Rule public TestName nameRule = new TestName();

  private Repository diskRepo;
  private TestRepository<Repository> repo;
  private RefDirectory refdir;
  private RevCommit A;
  private RevCommit B;

  @Mock SharedRefDatabaseWrapper sharedRefDatabase;

  @Mock SharedRefEnforcement tmpRefEnforcement;
  @Mock ProjectsFilter projectsFilter;
  @Mock OneParameterVoidFunction<List<ReceiveCommand>> rollbackFunction;

  @Before
  public void setup() throws Exception {
    super.setUp();
    lenient().doReturn(false).when(sharedRefDatabase).isUpToDate(any(), any());
    when(projectsFilter.matches(anyString())).thenReturn(true);
    gitRepoSetup();
  }

  private void gitRepoSetup() throws Exception {
    diskRepo = createBareRepository();
    refdir = (RefDirectory) diskRepo.getRefDatabase();
    repo = new TestRepository<>(diskRepo);
    A = repo.commit().create();
    updateRef(A_REF_NAME_1, A.getId());
    updateRef(A_REF_NAME_2, A.getId());
    B = repo.commit(repo.getRevWalk().parseCommit(A));
  }

  @Test
  public void shouldUpdateSharedRefDbForAllRefUpdates() throws IOException {
    BatchRefUpdate batchRefUpdate =
        newBatchUpdate(
            List.of(
                new ReceiveCommand(A, B, A_REF_NAME_1, UPDATE),
                new ReceiveCommand(A, B, A_REF_NAME_2, UPDATE)));
    BatchRefUpdateValidator batchRefUpdateValidator =
        getRefValidatorForEnforcement(tmpRefEnforcement);

    doReturn(SharedRefEnforcement.EnforcePolicy.REQUIRED)
        .when(batchRefUpdateValidator.refEnforcement)
        .getPolicy(A_TEST_PROJECT_NAME, A_REF_NAME_1);

    doReturn(true).when(sharedRefDatabase).isUpToDate(any(), any());
    doReturn(true).when(sharedRefDatabase).compareAndPut(any(), any(), any());

    batchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), rollbackFunction);

    verify(rollbackFunction, never()).invoke(any());

    List<ReceiveCommand> commands = batchRefUpdate.getCommands();
    assertThat(commands.size()).isEqualTo(2);
    commands.forEach(
        (command) -> assertThat(command.getResult()).isEqualTo(ReceiveCommand.Result.OK));
  }

  @Test
  public void immutableChangeShouldNotBeWrittenIntoSharedRefDb() throws Exception {
    String AN_IMMUTABLE_REF = "refs/changes/01/1/1";

    ReceiveCommand receiveCommand =
        new ReceiveCommand(ObjectId.zeroId(), B, AN_IMMUTABLE_REF, CREATE);
    BatchRefUpdate batchRefUpdate = newBatchUpdate(List.of(receiveCommand));

    BatchRefUpdateValidator BatchRefUpdateValidator = newDefaultValidator();
    BatchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), this::defaultRollback);

    verify(sharedRefDatabase, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    assertThatReceiveCommandIsSuccessful(receiveCommand);
  }

  @Test
  public void compareAndPutShouldAlwaysIngoreAlwaysDraftCommentsEvenOutOfOrder() throws Exception {
    String DRAFT_COMMENT = "refs/draft-comments/56/450756/1013728";
    ReceiveCommand receiveCommand = new ReceiveCommand(ObjectId.zeroId(), B, DRAFT_COMMENT, CREATE);

    BatchRefUpdate batchRefUpdate = newBatchUpdate(List.of(receiveCommand));
    BatchRefUpdateValidator BatchRefUpdateValidator = newDefaultValidator();

    BatchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), this::defaultRollback);

    verify(sharedRefDatabase, never())
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, newRef(DRAFT_COMMENT, A.getId()), B.getId());
    assertThatReceiveCommandIsSuccessful(receiveCommand);
  }

  @Test
  public void validationShouldFailWhenLocalRefDbIsOutOfSync() throws Exception {
    ReceiveCommand receiveCommand = new ReceiveCommand(A, B, A_REF_NAME_1, UPDATE);
    BatchRefUpdate batchRefUpdate = newBatchUpdate(singletonList(receiveCommand));
    BatchRefUpdateValidator batchRefUpdateValidator =
        getRefValidatorForEnforcement(tmpRefEnforcement);

    doReturn(SharedRefEnforcement.EnforcePolicy.REQUIRED)
        .when(batchRefUpdateValidator.refEnforcement)
        .getPolicy(A_TEST_PROJECT_NAME, A_REF_NAME_1);
    doReturn(false).when(sharedRefDatabase).isUpToDate(eq(A_TEST_PROJECT_NAME_KEY), any());
    doReturn(true).when(sharedRefDatabase).exists(eq(A_TEST_PROJECT_NAME_KEY), any());

    batchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), rollbackFunction);

    verify(rollbackFunction, never()).invoke(any());

    assertThat(receiveCommand.getResult()).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void shouldRollbackWhenSharedRefUpdateCompareAndPutThrowsUncaughtThrowable()
      throws Exception {
    ReceiveCommand receiveCommand = new ReceiveCommand(A, B, A_REF_NAME_1, UPDATE);
    BatchRefUpdate batchRefUpdate = newBatchUpdate(singletonList(receiveCommand));
    BatchRefUpdateValidator batchRefUpdateValidator =
        getRefValidatorForEnforcement(tmpRefEnforcement);

    doReturn(SharedRefEnforcement.EnforcePolicy.REQUIRED)
        .when(batchRefUpdateValidator.refEnforcement)
        .getPolicy(A_TEST_PROJECT_NAME, A_REF_NAME_1);
    doReturn(true).when(sharedRefDatabase).isUpToDate(any(), any());

    doThrow(TestError.class).when(sharedRefDatabase).compareAndPut(any(), any(), any());

    assertThrows(
        TestError.class,
        () ->
            batchRefUpdateValidator.executeBatchUpdateWithValidation(
                batchRefUpdate, () -> execute(batchRefUpdate), rollbackFunction));

    verify(rollbackFunction).invoke(any());
    assertThat(receiveCommand.getResult()).isEqualTo(ReceiveCommand.Result.LOCK_FAILURE);
  }

  @Test
  public void shouldRollbackRefUpdateWhenRefDbIsNotUpdated() throws Exception {
    ReceiveCommand receiveCommand = new ReceiveCommand(A, B, A_REF_NAME_1, UPDATE);
    BatchRefUpdate batchRefUpdate = newBatchUpdate(singletonList(receiveCommand));
    BatchRefUpdateValidator batchRefUpdateValidator =
        getRefValidatorForEnforcement(tmpRefEnforcement);

    doReturn(SharedRefEnforcement.EnforcePolicy.REQUIRED)
        .when(batchRefUpdateValidator.refEnforcement)
        .getPolicy(A_TEST_PROJECT_NAME, A_REF_NAME_1);

    doReturn(true).when(sharedRefDatabase).isUpToDate(any(), any());

    lenient()
        .doThrow(GlobalRefDbSystemError.class)
        .when(sharedRefDatabase)
        .compareAndPut(any(), any(), any());

    batchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), rollbackFunction);

    verify(rollbackFunction, times(1)).invoke(any());
    assertThat(receiveCommand.getResult()).isEqualTo(ReceiveCommand.Result.LOCK_FAILURE);
  }

  @Test
  public void shouldNotUpdateSharedRefDbWhenProjectIsLocal() throws Exception {
    when(projectsFilter.matches(anyString())).thenReturn(false);

    BatchRefUpdate batchRefUpdate =
        newBatchUpdate(singletonList(new ReceiveCommand(A, B, A_REF_NAME_1, UPDATE)));
    BatchRefUpdateValidator batchRefUpdateValidator =
        getRefValidatorForEnforcement(tmpRefEnforcement);

    batchRefUpdateValidator.executeBatchUpdateWithValidation(
        batchRefUpdate, () -> execute(batchRefUpdate), this::defaultRollback);

    verify(sharedRefDatabase, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
  }

  private void updateRef(String refName, ObjectId sha1) throws IOException {
    RefUpdate aUpdate = refdir.newUpdate(refName, false);
    aUpdate.setNewObjectId(sha1);
    aUpdate.forceUpdate();
  }

  private static void assertThatReceiveCommandIsSuccessful(ReceiveCommand receiveCommand) {
    assertThat(receiveCommand.getResult()).isEqualTo(Result.OK);
  }

  private BatchRefUpdateValidator newDefaultValidator() {
    return getRefValidatorForEnforcement(new DefaultSharedRefEnforcement());
  }

  private BatchRefUpdateValidator getRefValidatorForEnforcement(
      SharedRefEnforcement sharedRefEnforcement) {
    return new BatchRefUpdateValidator(
        sharedRefDatabase,
        new ValidationMetrics(
            new DisabledMetricMaker(), new SharedRefDbConfiguration(new Config(), "testplugin")),
        sharedRefEnforcement,
        new DummyLockWrapper(),
        projectsFilter,
        RefFixture.A_TEST_PROJECT_NAME,
        diskRepo.getRefDatabase(),
        ImmutableSet.of());
  }

  private void execute(BatchRefUpdate u) throws IOException {
    try (RevWalk rw = new RevWalk(diskRepo)) {
      u.execute(rw, NullProgressMonitor.INSTANCE);
    }
  }

  private BatchRefUpdate newBatchUpdate(List<ReceiveCommand> cmds) {
    BatchRefUpdate u = refdir.newBatchUpdate();
    u.addCommand(cmds);
    cmds.forEach(c -> c.setResult(Result.NOT_ATTEMPTED));
    return u;
  }

  private void defaultRollback(List<ReceiveCommand> cmds) throws IOException {
    // do nothing
  }

  @Override
  public String testBranch() {
    return "branch_" + nameRule.getMethodName();
  }

  private static class TestError extends Error {}
}
