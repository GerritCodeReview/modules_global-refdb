<<<<<<< PATCH SET (2c8e6d Replace Custom EnforcementRules with storeAllRefs/storeNoRef)
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

import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration.SharedRefDatabase;
import java.util.Arrays;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.junit.Before;
import org.junit.Test;

public class CustomSharedRefEnforcementByProjectTest implements RefFixture {

  SharedRefEnforcement refEnforcement;

  @Before
  public void setUp() {
    Config sharedRefDbConfig = new Config();
    sharedRefDbConfig.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.SUBSECTION_ENFORCEMENT_RULES,
        SharedRefEnforcement.Policy.EXCLUDE.name(),
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
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void projectOneEnforcementShouldAlwaysPrevail() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectOne", aRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aNonListedProjectShouldRequireRefForMasterTest() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", aRef.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void projectTwoSpecificRefShouldReturnExcludePolicy() {
    Ref refOne = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    Ref refTwo = newRef("refs/heads/master/test2", AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
    assertThat(refEnforcement.getPolicy("ProjectTwo", refTwo.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aNonListedProjectShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/newChange", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aNonListedRefInProjectShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aNonListedProjectAndRefShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForProjectOneShouldReturnExclude() {
    assertThat(refEnforcement.getPolicy("ProjectOne"))
        .isEqualTo(SharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void getProjectPolicyForProjectTwoShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("ProjectTwo"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForNonListedProjectShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForNonListedProjectWhenSingleProject() {
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(SharedRefEnforcement.Policy.EXCLUDE, ":refs/heads/master");

    assertThat(customEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getANonListedProjectWhenOnlyOneProjectIsListedShouldReturnInclude() {
    SharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(SharedRefEnforcement.Policy.EXCLUDE, "AProject:");
    assertThat(customEnforcement.getPolicy("NonListedProject", "refs/heads/master"))
        .isEqualTo(SharedRefEnforcement.Policy.INCLUDE);
  }

  private SharedRefEnforcement newCustomRefEnforcementWithValue(
      SharedRefEnforcement.Policy policy, String... projectAndRefs) {
    Config sharedRefDbConfiguration = new Config();
    sharedRefDbConfiguration.setStringList(
        SharedRefDatabase.SECTION,
        SharedRefDatabase.SUBSECTION_ENFORCEMENT_RULES,
        policy.name(),
        Arrays.asList(projectAndRefs));
    return newCustomRefEnforcement(sharedRefDbConfiguration);
  }

  private SharedRefEnforcement newCustomRefEnforcement(Config sharedRefDbConfig) {
    return new CustomSharedRefEnforcementByProject(
        new SharedRefDbConfiguration(sharedRefDbConfig, "testplugin"));
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

import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration.SharedRefDatabase;
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
        LegacySharedRefEnforcement.Policy.EXCLUDE.name(),
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
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void projectOneEnforcementShouldAlwaysPrevail() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectOne", aRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aNonListedProjectShouldRequireRefForMasterTest() {
    Ref aRef = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", aRef.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void projectTwoSpecificRefShouldReturnExcludePolicy() {
    Ref refOne = newRef("refs/heads/master/test", AN_OBJECT_ID_1);
    Ref refTwo = newRef("refs/heads/master/test2", AN_OBJECT_ID_1);

    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
    assertThat(refEnforcement.getPolicy("ProjectTwo", refTwo.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void aNonListedProjectShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/newChange", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aNonListedRefInProjectShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("ProjectTwo", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void aNonListedProjectAndRefShouldReturnInclude() {
    Ref refOne = newRef("refs/heads/master/test3", AN_OBJECT_ID_1);
    assertThat(refEnforcement.getPolicy("NonListedProject", refOne.getName()))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForProjectOneShouldReturnExclude() {
    assertThat(refEnforcement.getPolicy("ProjectOne"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.EXCLUDE);
  }

  @Test
  public void getProjectPolicyForProjectTwoShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("ProjectTwo"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForNonListedProjectShouldReturnInclude() {
    assertThat(refEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getProjectPolicyForNonListedProjectWhenSingleProject() {
    LegacySharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(
            LegacySharedRefEnforcement.Policy.EXCLUDE, ":refs/heads/master");

    assertThat(customEnforcement.getPolicy("NonListedProject"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  @Test
  public void getANonListedProjectWhenOnlyOneProjectIsListedShouldReturnInclude() {
    LegacySharedRefEnforcement customEnforcement =
        newCustomRefEnforcementWithValue(LegacySharedRefEnforcement.Policy.EXCLUDE, "AProject:");
    assertThat(customEnforcement.getPolicy("NonListedProject", "refs/heads/master"))
        .isEqualTo(LegacySharedRefEnforcement.Policy.INCLUDE);
  }

  private LegacySharedRefEnforcement newCustomRefEnforcementWithValue(
      LegacySharedRefEnforcement.Policy policy, String... projectAndRefs) {
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
        new SharedRefDbConfiguration(sharedRefDbConfig, "testplugin"));
  }

  @Override
  public String testBranch() {
    return "fooBranch";
  }
}
>>>>>>> BASE      (a29b75 Deprecate SharedRefEnforcement)
