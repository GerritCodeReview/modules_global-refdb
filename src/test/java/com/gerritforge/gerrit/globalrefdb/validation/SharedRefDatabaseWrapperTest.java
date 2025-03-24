// Copyright (C) 2023 The Android Open Source Project
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

import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDatabase;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.gerritforge.gerrit.globalrefdb.GlobalRefDbSystemError;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.NoopSharedRefDatabase;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.gerrit.metrics.Timer0.Context;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SharedRefDatabaseWrapperTest {

  @Mock private SharedRefDBMetrics metrics;
  @Mock SharedRefLogger sharedRefLogger;
  @Mock private Context context;
  @Mock private Ref ref;

  private SharedRefDatabaseWrapper objectUnderTest;
  private String refName = "refs/heads/master";
  private Project.NameKey projectName = Project.nameKey("test_project");

  @Before
  public void setup() {
    when(metrics.startCompareAndPutExecutionTime()).thenReturn(context);
    when(metrics.startLockRefExecutionTime()).thenReturn(context);
    when(metrics.startGetExecutionTime()).thenReturn(context);
    when(metrics.startExistsExecutionTime()).thenReturn(context);
    when(metrics.startIsUpToDateExecutionTime()).thenReturn(context);
    when(metrics.startRemoveExecutionTime()).thenReturn(context);
    objectUnderTest =
        new SharedRefDatabaseWrapper(sharedRefLogger, metrics, NoOpRefLocker.INSTANCE);
  }

  @Test
  public void shouldUpdateCompareAndPutExecutionTimeMetricWhenCompareAndPut() {
    objectUnderTest.compareAndPut(projectName, refName, ObjectId.zeroId(), ObjectId.zeroId());
    verify(metrics).startCompareAndPutExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateLockRefExecutionTimeMetricWhenLockRefIsCalled()
      throws GlobalRefDbLockException {
    objectUnderTest.lockRef(projectName, refName);
    verify(metrics).startLockRefExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateIsUpToDateExecutionTimeMetricWhenIsUpToDate()
      throws GlobalRefDbLockException {
    objectUnderTest.isUpToDate(projectName, ref);
    verify(metrics).startIsUpToDateExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateExistsExecutionTimeMetricWhenExistsIsCalled() {
    objectUnderTest.exists(projectName, refName);
    verify(metrics).startExistsExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateGetExecutionTimeMetricWhenGetIsCalled() {
    objectUnderTest.get(projectName, refName, String.class);
    verify(metrics).startGetExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldUpdateRemoveExecutionTimeMetricWhenRemoveCalled() {
    objectUnderTest.remove(projectName);
    verify(metrics).startRemoveExecutionTime();
    verify(context).close();
  }

  @Test
  public void shouldIncreaseNumberOfFailuresWhenCompareAndPutThrows() throws Exception {
    DynamicItem<GlobalRefDatabase> couldNotConnectGlobalRefDB =
        DynamicItem.itemOf(
            GlobalRefDatabase.class,
            new NoopSharedRefDatabase() {
              @Override
              public boolean compareAndPut(
                  Project.NameKey project, Ref currRef, ObjectId newRefValue)
                  throws GlobalRefDbSystemError {
                throw new GlobalRefDbSystemError(
                    "Could not write to global-refdb", new Exception("Could not connect"));
              }
            });
    objectUnderTest =
        new SharedRefDatabaseWrapper(
            couldNotConnectGlobalRefDB,
            new DisabledSharedRefLogger(),
            metrics,
            NoOpRefLocker.INSTANCE);

    assertThrows(
        GlobalRefDbSystemError.class,
        () -> objectUnderTest.compareAndPut(projectName, ref, ObjectId.zeroId()));

    verify(metrics).incrementOperationFailures();
  }
}
