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

import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.google.inject.Inject;
import java.util.List;

/**
 * Implementation of the {@link SharedRefEnforcement} interface which derives project and
 * project/ref enforcement policy from the configuration of the libModule consuming this library
 */
public class CustomSharedRefEnforcementByProject implements SharedRefEnforcement {
  private final SharedRefDbConfiguration config;
  private final List<Rule> enforcementRules;

  /**
   * Constructs a {@code CustomSharedRefEnforcementByProject} with the values specified in the
   * configuration of the libModule consuming this library
   *
   * @param config the libModule configuration
   */
  @Inject
  public CustomSharedRefEnforcementByProject(SharedRefDbConfiguration config) {
    this.config = config;
    this.enforcementRules = config.getSharedRefDb().getEnforcementRules();
  }

  /**
   * The enforcement policy for 'refName' in 'projectName' as computed from the libModule's
   * configuration file. We iterate over the whole list even if the project name is known; if we
   * expect a large number of rules we may prefer a Map<Project, Rule> instead later.
   *
   * <p>By default all projects are INCLUDE to be consistent on all refs.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the enforcement policy for this project/ref
   */
  @Override
  public Policy getPolicy(String projectName, String refName) {
    for (Rule rule : enforcementRules) {
      if (rule.matches(projectName, refName)) {
        return rule.getPolicy();
      }
    }
    return isRefToBeIgnoredBySharedRefDb(refName) ? Policy.EXCLUDE : Policy.INCLUDE;
  }

  /**
   * The enforcement policy for 'projectName' as computed from the libModule's configuration file.
   *
   * <p>By default all projects are INCLUDE to be consistent on all refs.
   *
   * @param projectName the name of the project to get the policy for
   * @return the enforcement policy for the project
   */
  @Override
  public Policy getPolicy(String projectName) {
    for (Rule rule : enforcementRules) {
      if (rule.matches(projectName, "*")) {
        return rule.getPolicy();
      }
    }
    return Policy.INCLUDE;
  }
}
