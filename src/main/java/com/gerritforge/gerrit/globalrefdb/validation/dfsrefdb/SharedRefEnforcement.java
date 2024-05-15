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
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;

/** Type of enforcement to implement between the local and shared RefDb. */
public class SharedRefEnforcement {
  public enum Policy {
    EXCLUDE,
    INCLUDE;
  }

  private final boolean storeAllRefs;

  @Inject
  public SharedRefEnforcement(SharedRefDbConfiguration config) {
    this.storeAllRefs = config.getSharedRefDb().isStoringAllRefs();
  }

  /**
   * The enforcement policy for 'refName' in 'projectName'
   *
   * <p>By default all projects are INCLUDE to be consistent on all refs.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the enforcement policy for this project/ref
   */
  public Policy getPolicy(String projectName, String refName) {
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
  public Policy getPolicy(String projectName) {
    return Policy.INCLUDE;
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
    if (storeAllRefs == true) {
      return false;
    }

    return refName == null
        || refName.startsWith("refs/draft-comments")
        || (refName.startsWith("refs/changes")
            && !refName.endsWith("/meta")
            && !refName.endsWith(RefNames.ROBOT_COMMENTS_SUFFIX))
        || refName.startsWith("refs/cache-automerge");
  }
}
