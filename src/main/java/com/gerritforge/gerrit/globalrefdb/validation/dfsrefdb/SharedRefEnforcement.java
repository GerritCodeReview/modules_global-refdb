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

import com.google.gerrit.entities.RefNames;

/** Type of enforcement to implement between the local and shared RefDb. */
public interface SharedRefEnforcement {
  enum Policy {
    INCLUDE,
    EXCLUDE;
  }

  class Rule {
    private final String projectWildcard;
    private final String refWildcard;
    private final Policy policy;

    public Rule(SharedRefEnforcement.Policy policy, String projectWildcard, String refWildcard) {
      this.policy = policy;
      this.projectWildcard = projectWildcard;
      this.refWildcard = refWildcard;
    }

    public boolean matches(String projectName, String refName) {
      return wildcardMatches(this.projectWildcard, projectName)
          && wildcardMatches(this.refWildcard, refName);
    }

    public Policy getPolicy() {
      return this.policy;
    }

    private boolean wildcardMatches(String wildcardPattern, String value) {
      return wildcardPattern.equals(value)
          || (wildcardPattern.endsWith("*")
              && value.startsWith(wildcardPattern.substring(0, wildcardPattern.length() - 1)));
    }
  }

  /**
   * Get the enforcement policy for a project/refName.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the {@link Policy} value
   */
  public Policy getPolicy(String projectName, String refName);

  /**
   * Get the enforcement policy for a project
   *
   * @param projectName the name of the project
   * @return the {@link Policy} value
   */
  public Policy getPolicy(String projectName);

  /**
   * Check if a refName should be ignored by global refdb. The default behaviour activates after any
   * custom rules, if no matches for the custom rules are found or no custom rules are provided.
   *
   * <ul>
   *   <li>refs/draft-comments :user-specific temporary storage that does not need to be seen by
   *       other users/sites
   *   <li>refs/changes/&lt;non-meta&gt;: those refs are immutable
   *   <li>refs/cache-automerge: these refs would be never replicated anyway
   * </ul>
   *
   * @param refName the name of the ref to check
   * @return true if ref should be ignored; false otherwise
   */
  default boolean isRefToBeIgnoredBySharedRefDb(String refName) {
    return refName == null
        || refName.startsWith("refs/draft-comments")
        || (refName.startsWith("refs/changes")
            && !refName.endsWith("/meta")
            && !refName.endsWith(RefNames.ROBOT_COMMENTS_SUFFIX))
        || refName.startsWith("refs/cache-automerge");
  }
}
