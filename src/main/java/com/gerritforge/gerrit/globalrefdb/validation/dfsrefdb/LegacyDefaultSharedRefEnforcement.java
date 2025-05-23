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
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

/**
 * Default implementation of {@link LegacySharedRefEnforcement}. This class provides the default
 * project/ref enforcement rules when no more specific rules have been configured for the libModule
 * consuming this library.
 */
@Deprecated(forRemoval = true)
public class LegacyDefaultSharedRefEnforcement implements LegacySharedRefEnforcement {

  private final Boolean enableDraftCommentEvents;

  @Inject
  public LegacyDefaultSharedRefEnforcement(
      DraftCommentEventsEnabledProvider draftCommentEventsEnabledProvider) {
    this.enableDraftCommentEvents = draftCommentEventsEnabledProvider.get();
  }

  @VisibleForTesting
  public LegacyDefaultSharedRefEnforcement(boolean enableDraftCommentEvents) {
    this.enableDraftCommentEvents = enableDraftCommentEvents;
  }

  @VisibleForTesting
  public LegacyDefaultSharedRefEnforcement() {
    this.enableDraftCommentEvents = false;
  }

  /**
   * Returns {@link EnforcePolicy#IGNORED} for refs to be ignored {@link
   * LegacySharedRefEnforcement#isRefToBeIgnoredBySharedRefDb(String)}, {@link
   * EnforcePolicy#REQUIRED} otherwise
   *
   * @param projectName project to be enforced
   * @param refName ref name to be enforced
   * @return the policy for this project/ref
   */
  @Override
  public EnforcePolicy getPolicy(String projectName, String refName) {
    return isRefToBeIgnoredBySharedRefDb(refName) ? EnforcePolicy.IGNORED : EnforcePolicy.REQUIRED;
  }

  /**
   * The global refdb validation policy for 'projectName'
   *
   * @param projectName project to be enforced
   * @return always {@link EnforcePolicy#REQUIRED}
   */
  @Override
  public EnforcePolicy getPolicy(String projectName) {
    return EnforcePolicy.REQUIRED;
  }

  @Override
  public Boolean isDraftCommentEventsEnabled() {
    return enableDraftCommentEvents;
  }
}
