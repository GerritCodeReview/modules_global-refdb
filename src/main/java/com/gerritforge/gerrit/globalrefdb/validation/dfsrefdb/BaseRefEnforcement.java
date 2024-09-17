// Copyright (C) 2024 The Android Open Source Project
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

import com.google.gerrit.server.config.GerritServerConfig;
import org.eclipse.jgit.lib.Config;

/** Base class of {@link SharedRefEnforcement}, providing a common constructor */
public class BaseRefEnforcement {

  protected final boolean enableDraftCommentEvents;

  public BaseRefEnforcement(@GerritServerConfig Config gerritConfig) {
    this.enableDraftCommentEvents =
        gerritConfig.getBoolean("event", "stream-events", "enableDraftCommentEvents", false);
  }
}
