<<<<<<< PATCH SET (b4ef11 Replace Custom EnforcementRules with storeAllRefs/storeNoRef)
||||||| BASE
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

package com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb;

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.entities.RefNames;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

public class DefaultSharedRefEnforcementTest implements RefFixture {

  LegacySharedRefEnforcement refEnforcement = new DefaultSharedRefEnforcement();

  @Test
  public void anImmutableChangeShouldBeIgnored() {
    Ref immutableChangeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aChangeMetaShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/changes/01/1/meta", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aChangeRobotCommentsShouldNotBeIgnored() {
    Ref robotCommentsMutableRef =
        newRef("refs/changes/01/1" + RefNames.ROBOT_COMMENTS_SUFFIX, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, robotCommentsMutableRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aCacheAutomergeShouldBeIgnored() {
    Ref immutableChangeRef = newRef("refs/cache-automerge/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aDraftCommentsShouldBeIgnored() {
    Ref immutableChangeRef = newRef("refs/draft-comments/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void regularRefHeadsMasterShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/heads/master", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void regularCommitShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/heads/stable-2.16", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void allUsersExternalIdsRefShouldBeRequired() {
    Ref refOne = newRef("refs/meta/external-ids", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("All-Users", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Override
  public String testBranch() {
    return "fooBranch";
  }
}
=======
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

package com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb;

import static com.google.common.truth.Truth.assertThat;

import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.EnforcePolicy;
import com.google.gerrit.entities.RefNames;
import org.eclipse.jgit.lib.Ref;
import org.junit.Test;

public class DefaultSharedRefEnforcementTest implements RefFixture {

  LegacySharedRefEnforcement refEnforcement = new DefaultSharedRefEnforcement();

  @Test
  public void anImmutableChangeShouldBeIgnored() {
    Ref immutableChangeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aChangeMetaShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/changes/01/1/meta", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aChangeRobotCommentsShouldNotBeIgnored() {
    Ref robotCommentsMutableRef =
        newRef("refs/changes/01/1" + RefNames.ROBOT_COMMENTS_SUFFIX, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, robotCommentsMutableRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aCacheAutomergeShouldBeIgnored() {
    Ref immutableChangeRef = newRef("refs/cache-automerge/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aDraftCommentsShouldBeIgnored() {
    Ref immutableChangeRef = newRef("refs/draft-comments/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void regularRefHeadsMasterShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/heads/master", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void regularCommitShouldNotBeIgnored() {
    Ref immutableChangeRef = newRef("refs/heads/stable-2.16", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void allUsersExternalIdsRefShouldBeRequired() {
    Ref refOne = newRef("refs/meta/external-ids", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("All-Users", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Override
  public String testBranch() {
    return "fooBranch";
  }
}
>>>>>>> BASE      (c2bf1c Deprecate SharedRefEnforcement)
