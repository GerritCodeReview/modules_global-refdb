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
import java.util.Map;

/**
 * Implementation of the {@link SharedRefEnforcement} interface which derives project and
 * project/ref enforcement policy from the configuration of the libModule consuming this library
 */
public class CustomSharedRefEnforcementByProject implements SharedRefEnforcement {
  private static final String ALL = "*";

  private final SharedRefDbConfiguration config;
  private final Map<String, List<Rule>> enforcementRulesMap;

  /**
   * Constructs a {@code CustomSharedRefEnforcementByProject} with the values specified in the
   * configuration of the libModule consuming this library
   *
   * @param config the libModule configuration
   */
  @Inject
  public CustomSharedRefEnforcementByProject(SharedRefDbConfiguration config) {
    this.config = config;
    this.enforcementRulesMap = config.getSharedRefDb().getEnforcementRules();
  }

  /**
   * The enforcement policy for 'refName' in 'projectName' as computed from the libModule's
   * configuration file. We iterate over the list of rules associated to the given project.
   *
   * <p>By default all projects are INCLUDE to be consistent on all refs.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the enforcement policy for this project/ref
   */
  @Override
  public SharedRefEnforcement.Policy getPolicy(String projectName, String refName) {
    List<Rule> rules =
        enforcementRulesMap.getOrDefault(
            projectName, enforcementRulesMap.getOrDefault(ALL, List.of()));

    for (Rule rule : rules) {
      if (rule.matches(projectName, refName)) {
        return rule.getPolicy();
      }
    }
    return isRefToBeIgnoredBySharedRefDb(refName)
        ? SharedRefEnforcement.Policy.EXCLUDE
        : SharedRefEnforcement.Policy.INCLUDE;
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
  public SharedRefEnforcement.Policy getPolicy(String projectName) {
    return getPolicy(projectName, ALL);
  }
}
