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

import com.gerritforge.gerrit.globalrefdb.DraftCommentEventsEnabledProvider;
import com.gerritforge.gerrit.globalrefdb.validation.SharedRefDbConfiguration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.RefNames;
import com.google.inject.Inject;

/** Type of enforcement to implement between the local and shared RefDb. */
public class SharedRefEnforcement {
  public enum Policy {
    EXCLUDE,
    INCLUDE_MUTABLE,
    INCLUDE;
  }

  private final ImmutableSet<String> storeAllRefs;
  private final ImmutableSet<String> storeMutableRefs;
  private final ImmutableSet<String> storeNoRefs;
  private final Boolean enableDraftCommentEvents;
  private final String ALL = "*";

  @Inject
  public SharedRefEnforcement(
      SharedRefDbConfiguration config,
      DraftCommentEventsEnabledProvider draftCommentEventsEnabledProvider) {
    this.storeAllRefs = config.getSharedRefDb().getStoreAllRefs();
    this.storeMutableRefs = config.getSharedRefDb().getStoreMutableRefs();
    this.storeNoRefs = config.getSharedRefDb().getStoreNoRefs();
    this.enableDraftCommentEvents = draftCommentEventsEnabledProvider.get();
  }

  @VisibleForTesting
  public SharedRefEnforcement(
      ImmutableSet<String> storeAllRefs,
      ImmutableSet<String> storeMutableRefs,
      ImmutableSet<String> storeNoRefs,
      boolean enableDraftCommentEvents) {
    this.storeAllRefs = storeAllRefs;
    this.storeMutableRefs = storeMutableRefs;
    this.storeNoRefs = storeNoRefs;
    this.enableDraftCommentEvents = enableDraftCommentEvents;
  }

  @VisibleForTesting
  public SharedRefEnforcement() {
    this.storeAllRefs = ImmutableSet.of();
    this.storeMutableRefs = ImmutableSet.of();
    this.storeNoRefs = ImmutableSet.of();
    this.enableDraftCommentEvents = false;
  }

  /**
   * The enforcement policy for 'refName' in 'projectName'
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the enforcement policy for this project/ref
   */
  public Policy getPolicy(String projectName, String refName) {
    Policy configuredPolicy = getPolicy(projectName);
    if (configuredPolicy == Policy.INCLUDE_MUTABLE) {
      return isRefToBeIgnoredBySharedRefDb(refName) ? Policy.EXCLUDE : Policy.INCLUDE;
    }
    return configuredPolicy;
  }

  /**
   * Returns the configured enforcement policy for a project. First checks the project-specific
   * settings, then the global projects setting. Priority order is storeNoRefs over storeMutableRefs
   * and storeAllRefs.
   *
   * <p>If no specific policy is configured, defaults to storing mutable refs.
   *
   * @param projectName the name of the project to get the policy for
   * @return the enforcement policy for the project
   */
  public Policy getPolicy(String projectName) {
    if (storeNoRefs.contains(projectName)) {
      return Policy.EXCLUDE;
    }
    if (storeMutableRefs.contains(projectName)) {
      return Policy.INCLUDE_MUTABLE;
    }
    if (storeAllRefs.contains(projectName)) {
      return Policy.INCLUDE;
    }

    if (storeNoRefs.contains(ALL)) {
      return Policy.EXCLUDE;
    }
    if (storeMutableRefs.contains(ALL)) {
      return Policy.INCLUDE_MUTABLE;
    }
    if (storeAllRefs.contains(ALL)) {
      return Policy.INCLUDE;
    }
    return Policy.INCLUDE_MUTABLE;
  }

  /**
   * Returns whether draft comment events are enabled.
   *
   * @return true if draft comment events are enabled, false otherwise
   */
  public Boolean isDraftCommentEventsEnabled() {
    return enableDraftCommentEvents;
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
        || (refName.startsWith("refs/draft-comments") && !enableDraftCommentEvents)
        || (refName.startsWith("refs/changes")
            && !refName.endsWith("/meta")
            && !refName.endsWith(RefNames.ROBOT_COMMENTS_SUFFIX))
        || refName.startsWith("refs/cache-automerge");
  }
}
