// Copyright (C) 2024 The Android Open Source Project
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

import com.gerritforge.gerrit.globalrefdb.DraftCommentEventsEnabledProvider;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration.SharedRefDatabase;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.Policy;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.RefNames;
import java.util.Arrays;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

public class SharedRefEnforcementTest implements RefFixture {

  SharedRefEnforcement refEnforcement;

  @Before
  public void setUp() {
    Config sharedRefDbConfig = new Config();
    refEnforcement = newRefEnforcement(sharedRefDbConfig);
  }

  @Test
  public void shouldProcessStoreNoRefsBeforeStoreAllRefs() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_NO_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList(A_TEST_PROJECT_NAME));
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_ALL_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList(A_TEST_PROJECT_NAME));

    SharedRefEnforcement refEnforcement = newRefEnforcement(sharedRefDbConfig);
    Ref changeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, changeRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void shouldNotIncludePatchSetRefWhenStoringNoRefs() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_NO_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList(A_TEST_PROJECT_NAME));

    SharedRefEnforcement refEnforcement = newRefEnforcement(sharedRefDbConfig);
    Ref changeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, changeRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void shouldIncludePatchSetRefWhenStoringAllRefs() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_ALL_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList("*"));

    SharedRefEnforcement refEnforcementIncludeAll = newRefEnforcement(sharedRefDbConfig);

    Ref immutableChangeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);
    Ref draftCommentRef = newRef("refs/draft-comments/01/1/1000000", AN_OBJECT_ID_1);
    Ref cacheAutomergeRef = newRef("refs/cache-automerge/01/1/1000000", AN_OBJECT_ID_1);

    assertThat(
            refEnforcementIncludeAll.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
    assertThat(refEnforcementIncludeAll.getPolicy(A_TEST_PROJECT_NAME, draftCommentRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
    assertThat(refEnforcementIncludeAll.getPolicy(A_TEST_PROJECT_NAME, cacheAutomergeRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void shouldExcludePatchSetRefWhenStoringMutableRefs() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_MUTABLE_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList(A_TEST_PROJECT_NAME));
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.STORE_ALL_REFS_KEY,
        SharedRefDatabase.PROJECT,
        Arrays.asList("*"));

    SharedRefEnforcement refEnforcement = newRefEnforcement(sharedRefDbConfig);
    Ref changeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, changeRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void patchSetRefIsExcludedByDefault() {
    Ref changeRef = newRef(A_REF_NAME_OF_A_PATCHSET, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, changeRef.getName()))
        .isEqualTo(Policy.EXCLUDE);
  }

  @Test
  public void changeMetaRefIncludedByDefault() {
    Ref changeRef = newRef("refs/changes/01/1/meta", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, changeRef.getName()))
        .isEqualTo(Policy.INCLUDE);
  }

  @Test
  public void changeRobotCommentsIncludedByDefault() {
    Ref robotCommentsMutableRef =
        newRef("refs/changes/01/1" + RefNames.ROBOT_COMMENTS_SUFFIX, AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, robotCommentsMutableRef.getName()))
        .isEqualTo(Policy.INCLUDE);
  }

  @Test
  public void cacheAutomergeExcludedByDefault() {
    Ref immutableChangeRef = newRef("refs/cache-automerge/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(Policy.EXCLUDE);
  }

  @Test
  public void draftCommentExcludedByDefault() {
    Ref immutableChangeRef = newRef("refs/draft-comments/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(Policy.EXCLUDE);
  }

  @Test
  public void regularRefHeadsMasterIncludedByDefault() {
    Ref immutableChangeRef = newRef("refs/heads/master", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(Policy.INCLUDE);
  }

  @Test
  public void regularCommitIncludedByDefault() {
    Ref immutableChangeRef = newRef("refs/heads/stable-2.16", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, immutableChangeRef.getName()))
        .isEqualTo(Policy.INCLUDE);
  }

  @Test
  public void allUsersExternalIdsRefIncludedByDefault() {
    Ref refOne = newRef("refs/meta/external-ids", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("All-Users", refOne.getName())).isEqualTo(Policy.INCLUDE);
  }

  @Test
  public void draftCommentsIncludedWhenDraftCommentEventsEnabled() {
    SharedRefEnforcement refEnforcement =
        new SharedRefEnforcement(ImmutableSet.of(), ImmutableSet.of(), ImmutableSet.of(), true);
    Ref draftCommentRef = newRef("refs/draft-comments/01/1/1000000", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy(A_TEST_PROJECT_NAME, draftCommentRef.getName()))
        .isEqualTo(Policy.INCLUDE);
  }

  private SharedRefEnforcement newRefEnforcement(Config sharedRefDbConfig) {
    return new SharedRefEnforcement(
        new SharedRefDbConfiguration(sharedRefDbConfig, "testplugin"),
        new DraftCommentEventsEnabledProvider(sharedRefDbConfig));
  }

  @Override
  public String testBranch() {
    return "fooBranch";
  }
}
