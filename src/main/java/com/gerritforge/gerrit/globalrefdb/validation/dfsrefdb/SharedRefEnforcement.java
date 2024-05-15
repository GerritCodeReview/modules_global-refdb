<<<<<<< PATCH SET (2c8e6d Replace Custom EnforcementRules with storeAllRefs/storeNoRef)
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
import com.google.common.collect.ImmutableList;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;
import java.util.Optional;

/** Type of enforcement to implement between the local and shared RefDb. */
public class SharedRefEnforcement {
  public enum Policy {
    EXCLUDE,
    INCLUDE;
  }

  private final ImmutableList<String> storeAllRefs;
  private final ImmutableList<String> storeNoRefs;
  private final String ALL = "*";

  @Inject
  public SharedRefEnforcement(SharedRefDbConfiguration config) {
    this.storeAllRefs = config.getSharedRefDb().getStoreAllRefs();
    this.storeNoRefs = config.getSharedRefDb().getStoreNoRefs();
  }

  /**
   * The enforcement policy for 'refName' in 'projectName'
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the enforcement policy for this project/ref
   */
  public Policy getPolicy(String projectName, String refName) {
    return getConfiguredPolicy(projectName)
        .orElse(isRefToBeIgnoredBySharedRefDb(refName) ? Policy.EXCLUDE : Policy.INCLUDE);
  }

  /**
   * The enforcement policy for 'projectName'. By default all projects are INCLUDE to be consistent
   * on all refs.
   *
   * @param projectName the name of the project to get the policy for
   * @return the enforcement policy for the project
   */
  public Policy getPolicy(String projectName) {
    return getConfiguredPolicy(projectName).orElse(Policy.INCLUDE);
  }

  Optional<Policy> getConfiguredPolicy(String projectName) {
    if (storeNoRefs.contains(ALL) || storeNoRefs.contains(projectName)) {
      return Optional.of(Policy.EXCLUDE);
    }
    if (storeAllRefs.contains(ALL) || storeAllRefs.contains(projectName)) {
      return Optional.of(Policy.INCLUDE);
    }
    return Optional.empty();
  }

  /**
   * Check if a refName should be ignored by global refdb. These rules apply when not storing all
   * refs.
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
  boolean isRefToBeIgnoredBySharedRefDb(String refName) {
    return refName == null
        || refName.startsWith("refs/draft-comments")
        || (refName.startsWith("refs/changes")
            && !refName.endsWith("/meta")
            && !refName.endsWith(RefNames.ROBOT_COMMENTS_SUFFIX))
        || refName.startsWith("refs/cache-automerge");
  }
}
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

import com.google.gerrit.entities.RefNames;

/** Type of enforcement to implement between the local and shared RefDb. */
public interface SharedRefEnforcement {
  enum Policy {
    EXCLUDE,
    INCLUDE;
  }

  /**
   * Get the enforcement policy for a project/refName.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the {@link Policy} value
   */
  Policy getPolicy(String projectName, String refName);

  /**
   * Get the enforcement policy for a project
   *
   * @param projectName the name of the project
   * @return the {@link Policy} value
   */
  Policy getPolicy(String projectName);

  /**
   * Check if a refName should be ignored by global refdb. The Default behaviour is to ignore:
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
=======
>>>>>>> BASE      (a29b75 Deprecate SharedRefEnforcement)
