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

import com.gerritforge.gerrit.globalrefdb.DraftCommentEventsEnabledProvider;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration.SharedRefDatabase;
import com.gerritforge.gerrit.globalrefdb.validation.dfsrefdb.SharedRefEnforcement.EnforcePolicy;
import java.util.Arrays;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

public class CustomSharedRefEnforcementByProjectTest implements RefFixture {

  LegacySharedRefEnforcement refEnforcement;

  @Before
  public void setUp() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.SUBSECTION_ENFORCEMENT_RULES,
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        LegacySharedRefEnforcement.Policy.EXCLUDE.name(),
||||||| BASE
        SharedRefEnforcement.Policy.EXCLUDE.name(),
=======
        EnforcePolicy.IGNORED.name(),
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
        Arrays.asList(
            "ProjectOne",
            "ProjectTwo:refs/heads/master/test",
            "ProjectTwo:refs/heads/master/test2"));

    refEnforcement = newCustomRefEnforcement(sharedRefDbConfig);
  }

  @Test
  public void projectOneShouldReturnDesiredForAllRefs() {
    Ref aRef = newRef("refs/heads/master/2", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectOne", aRef.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
=======
        .isEqualTo(EnforcePolicy.IGNORED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void projectOneEnforcementShouldAlwaysPrevail() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectOne", aRef.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
=======
        .isEqualTo(EnforcePolicy.IGNORED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void aNonListedProjectShouldRequireRefForMasterTest() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", aRef.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
        .isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void projectTwoSpecificRefShouldReturnIgnoredPolicy() {
    Ref refOne = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    Ref refTwo = newRef("refs/heads/master/test2", AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
=======
        .isEqualTo(EnforcePolicy.IGNORED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
    assertThat(refEnforcement.getPolicy("ProjectTwo", refTwo.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
=======
        .isEqualTo(EnforcePolicy.IGNORED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void aNonListedProjectShouldReturnRequired() {
    Ref refOne = newRef("refs/heads/master/newChange", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
        .isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void aNonListedRefInProjectShouldReturnRequired() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
        .isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void aNonListedProjectAndRefShouldReturnRequired() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
        .isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
  public void getProjectPolicyForProjectOneShouldReturnExclude() {
    assertThat(refEnforcement.getPolicy("ProjectOne"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
||||||| BASE
  public void getProjectPolicyForProjectOneShouldReturnExclude() {
    assertThat(refEnforcement.getPolicy("ProjectOne"))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
=======
  public void getProjectPolicyForProjectOneShouldReturnIgnored() {
    assertThat(refEnforcement.getPolicy("ProjectOne")).isEqualTo(EnforcePolicy.IGNORED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
  public void getProjectPolicyForProjectTwoShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("ProjectTwo"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
  public void getProjectPolicyForProjectTwoShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("ProjectTwo"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
  public void getProjectPolicyForProjectTwoShouldReturnRequired() {
    assertThat(refEnforcement.getPolicy("ProjectTwo")).isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
  public void getProjectPolicyForNonListedProjectShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
  public void getProjectPolicyForNonListedProjectShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
  public void getProjectPolicyForNonListedProjectShouldReturnRequired() {
    assertThat(refEnforcement.getPolicy("NonListedProject")).isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
  public void getProjectPolicyForNonListedProjectWhenSingleProject() {
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
    LegacySharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(
            LegacySharedRefEnforcement.Policy.EXCLUDE, ":refs/heads/master");
||||||| BASE
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(SharedRefEnforcement.Policy.EXCLUDE, ":refs/heads/master");
=======
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(EnforcePolicy.IGNORED, ":refs/heads/master");
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)

<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
    assertThat(customEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
    assertThat(customEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
    assertThat(customEnforcement.getPolicy("NonListedProject")).isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

  @Test
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
  public void getANonListedProjectWhenOnlyOneProjectIsListedShouldReturnInclude() {
    LegacySharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(LegacySharedRefEnforcement.Policy.EXCLUDE, "AProject:");
||||||| BASE
  public void getANonListedProjectWhenOnlyOneProjectIsListedShouldReturnInclude() {
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(SharedRefEnforcement.Policy.EXCLUDE, "AProject:");
=======
  public void getANonListedProjectWhenOnlyOneProjectIsListedShouldReturnRequired() {
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(EnforcePolicy.IGNORED, "AProject:");
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
    assertThat(customEnforcement.getPolicy("NonListedProject", "refs/heads/master"))
<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
||||||| BASE
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
=======
        .isEqualTo(EnforcePolicy.REQUIRED);
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
  }

<<<<<<< PATCH SET (a29b75 Deprecate SharedRefEnforcement)
  private LegacySharedRefEnforcement newCustomRefEnforcementWithValue(
      LegacySharedRefEnforcement.Policy policy, String... projectAndRefs) {
||||||| BASE
  private SharedRefEnforcement newCustomRefEnforcementWithValue(
      SharedRefEnforcement.Policy policy, String... projectAndRefs) {
=======
  private SharedRefEnforcement newCustomRefEnforcementWithValue(
      EnforcePolicy policy, String... projectAndRefs) {
>>>>>>> BASE      (d4e251 Suppress unused parameter warning)
    Config sharedRefDbConfiguration = new Config();
    sharedRefDbConfiguration.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.SUBSECTION_ENFORCEMENT_RULES,
        policy.name(),
        Arrays.asList(projectAndRefs));
    return newCustomRefEnforcement(sharedRefDbConfiguration);
  }

  private LegacySharedRefEnforcement newCustomRefEnforcement(Config sharedRefDbConfig) {
    return new CustomSharedRefEnforcementByProject(
        new SharedRefDbConfiguration(sharedRefDbConfig, "testplugin"),
        new DraftCommentEventsEnabledProvider(new Config()));
  }

  @Override
  public String testBranch() {
    return "fooBranch";
  }
}
