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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.RefDbLockException;
import com.gerritforge.gerrit.globalrefdb.validation.RefUpdateValidator.OneParameterFunction;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.DefaultSharedRefEnforcement;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.RefFixture;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
import java.io.IOException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectIdRef;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RefUpdateValidatorTest implements RefFixture {
  private static final DefaultSharedRefEnforcement defaultRefEnforcement =
      new DefaultSharedRefEnforcement();

  @Mock SharedRefDatabaseWrapper sharedRefDb;

  @Mock SharedRefLogger sharedRefLogger;

  @Mock SharedRefDBMetrics sharedRefDBMetrics;

  @Mock RefDatabase localRefDb;

  @Mock ValidationMetrics validationMetrics;

  @Mock RefUpdate refUpdate;

  @Mock ProjectsFilter projectsFilter;

  @Mock OneParameterFunction<ObjectId, Result> rollbackFunction;

  @Mock AutoCloseable lock;

  String refName;
  Ref oldUpdateRef;
  Ref newUpdateRef;
  Ref localRef;

  RefUpdateValidator refUpdateValidator;

  @Before
  public void setupMocks() throws Exception {
    refName = aBranchRef();
    oldUpdateRef = newRef(refName, AN_OBJECT_ID_1);
    newUpdateRef = newRef(refName, AN_OBJECT_ID_2);
    localRef = newRef(refName, AN_OBJECT_ID_3);

    doReturn(localRef).when(localRefDb).findRef(refName);
    doReturn(localRef).when(localRefDb).exactRef(refName);
    doReturn(newUpdateRef.getObjectId()).when(refUpdate).getNewObjectId();
    doReturn(refName).when(refUpdate).getName();
    lenient().doReturn(oldUpdateRef.getObjectId()).when(refUpdate).getOldObjectId();

    doReturn(true).when(projectsFilter).matches(anyString());
    doReturn(Result.FAST_FORWARD).when(rollbackFunction).invoke(any());

    refUpdateValidator = newRefUpdateValidator(sharedRefDb);
  }

  @Test
  public void validationShouldSucceedWhenSharedRefDbIsNoop() throws Exception {
    SharedRefDatabaseWrapper noopSharedRefDbWrapper =
        new SharedRefDatabaseWrapper(sharedRefLogger, sharedRefDBMetrics, NoOpRefLocker.INSTANCE);

    Result result =
        newRefUpdateValidator(noopSharedRefDbWrapper)
            .executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);
    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void validationShouldSucceedWhenLocalRefDbIsUpToDate() throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, newUpdateRef.getObjectId());

    Result result =
        refUpdateValidator.executeRefUpdate(
            refUpdate, () -> doLocalRefUpdate(refName), this::defaultRollback);

    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void sharedRefDbShouldBeUpdatedWithRefDeleted() throws Exception {
    doReturn(ObjectId.zeroId()).when(refUpdate).getNewObjectId();
    doReturn(true).when(sharedRefDb).isUpToDate(any(Project.NameKey.class), any(Ref.class));
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, ObjectId.zeroId());
    doReturn(localRef).doReturn(null).when(localRefDb).findRef(refName);

    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.FORCED, this::defaultRollback);

    assertThat(result).isEqualTo(Result.FORCED);
  }

  @Test
  public void sharedRefDbShouldBeUpdatedWithNewRefCreated() throws Exception {
    Ref localNullRef = nullRef(refName);

    doReturn(true).when(sharedRefDb).isUpToDate(any(Project.NameKey.class), any(Ref.class));
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localNullRef, newUpdateRef.getObjectId());
    doReturn(localNullRef).doReturn(newUpdateRef).when(localRefDb).findRef(refName);

    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);

    assertThat(result).isEqualTo(Result.NEW);
  }

  @Test
  public void validationShouldFailWhenLocalRefDbIsOutOfSync() throws Exception {
    lenient()
        .doReturn(true)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).exists(A_TEST_PROJECT_NAME_KEY, refName);
    doReturn(false).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);

    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);

    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void validationShouldFailWhenLocalRefDbIsLocked() throws Exception {
    doThrow(RefDbLockException.class)
        .when(sharedRefDb)
        .lockLocalRef(A_TEST_PROJECT_NAME_KEY, refName);

    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);

    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void shouldRollbackWhenLocalRefDbIsUpToDateButFinalCompareAndPutIsFailing()
      throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);
    lenient()
        .doReturn(true)
        .when(sharedRefDb)
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    doReturn(false)
        .when(sharedRefDb)
        .compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, AN_OBJECT_ID_2);
    doReturn(lock).when(sharedRefDb).lockRef(any(), anyString());

    Result result =
        refUpdateValidator.executeRefUpdate(
            refUpdate, () -> doLocalRefUpdate(refName), rollbackFunction);

    verify(sharedRefDb).compareAndPut(A_TEST_PROJECT_NAME_KEY, localRef, AN_OBJECT_ID_2);
    verify(rollbackFunction).invoke(any());
    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void shouldNotUpdateSharedRefDbWhenFinalCompareAndPutIsFailing() throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);

    Result result =
        refUpdateValidator.executeRefUpdate(
            refUpdate, () -> Result.LOCK_FAILURE, this::defaultRollback);

    verify(sharedRefDb, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
  }

  @Test
  public void shouldRollbackRefUpdateCompareAndPutIsFailing() throws Exception {
    lenient()
        .doReturn(false)
        .when(sharedRefDb)
        .isUpToDate(any(Project.NameKey.class), any(Ref.class));
    doReturn(true).when(sharedRefDb).isUpToDate(A_TEST_PROJECT_NAME_KEY, localRef);
    when(sharedRefDb.compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class)))
        .thenThrow(GlobalRefDbSystemError.class);
    when(rollbackFunction.invoke(any())).thenReturn(Result.LOCK_FAILURE);

    Result result =
        refUpdateValidator.executeRefUpdate(
            refUpdate, () -> doLocalRefUpdate(localRef.getName()), rollbackFunction);

    verify(rollbackFunction).invoke(any());
  }

  @Test
  public void shouldSucceedButNotUpdateSharedRefDbWhenProjectIsLocal() throws Exception {
    when(projectsFilter.matches(anyString())).thenReturn(false);

    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);

    assertThat(result).isEqualTo(Result.NEW);
    verify(sharedRefDb, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
  }

  @Test
  public void shouldReturnLockFailureAndNotUpdateSharedRefDbWhenLocalRefUpdateNotExecuted()
      throws Exception {
    Result result =
        refUpdateValidator.executeRefUpdate(refUpdate, () -> Result.NEW, this::defaultRollback);

    assertThat(result).isEqualTo(Result.LOCK_FAILURE);
    verify(sharedRefDb, never())
        .compareAndPut(any(Project.NameKey.class), any(Ref.class), any(ObjectId.class));
  }

  private Result defaultRollback(ObjectId objectId) {
    return Result.NO_CHANGE;
  }

  private Result doLocalRefUpdate(String refName) throws IOException {
    lenient()
        .doReturn(new ObjectIdRef.Unpeeled(Ref.Storage.LOOSE, refName, AN_OBJECT_ID_2))
        .when(localRefDb)
        .findRef(refName);
    return Result.NEW;
  }

  private RefUpdateValidator newRefUpdateValidator(SharedRefDatabaseWrapper refDbWrapper) {
    return new RefUpdateValidator(
        refDbWrapper,
        validationMetrics,
        defaultRefEnforcement,
        projectsFilter,
        A_TEST_PROJECT_NAME,
        localRefDb,
        ImmutableSet.of());
  }
}
