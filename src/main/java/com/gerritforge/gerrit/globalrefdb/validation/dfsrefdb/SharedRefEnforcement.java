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
import java.util.Map;
import java.util.regex.Pattern;

/** Type of enforcement to implement between the local and shared RefDb. */
public interface SharedRefEnforcement {
  public enum EnforcePolicy {
    IGNORED,
    REQUIRED;
  }

  public enum StorageRule {
    INCLUDE,
    EXCLUDE;
  }

  /**
   * Get the enforcement policy for a project/refName.
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the {@link EnforcePolicy} value
   */
  public EnforcePolicy getPolicy(String projectName, String refName);

  /**
   * Get the enforcement policy for a project
   *
   * @param projectName the name of the project
   * @return the {@link EnforcePolicy} value
   */
  public EnforcePolicy getPolicy(String projectName);

  /**
   * Check if a refName should be ignored by global refdb. The default behaviour activates when
   * customPatterns is null:
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
  default boolean isRefToBeIgnoredBySharedRefDb(String refName, SharedRefDbConfiguration config) {
    for (Map.Entry<String, String> entry : config.getSharedRefDb().getStorageRules()) {
      // pattern match the refName to the value of the rule entry
      if (Pattern.matches(entry.getValue(), refName)) {
        if (entry.getKey().equals(StorageRule.EXCLUDE.name())) {
          return true;
        } else if (entry.getKey().equals(StorageRule.INCLUDE.name())) {
          return false;
        }
      }
    }
    return false;
  }
}
