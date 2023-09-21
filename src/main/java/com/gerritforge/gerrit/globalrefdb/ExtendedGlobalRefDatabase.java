// Copyright (C) 2023 GerritForge Ltd
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

package com.gerritforge.gerrit.globalrefdb;

import com.google.gerrit.entities.Project;

public interface ExtendedGlobalRefDatabase extends GlobalRefDatabase {

  /**
   * Set a value of generic type T.
   *
   * <p>Set is executed as an atomic operation.
   *
   * @param project project name of the ref.
   * @param refName to store the value for.
   * @param newValue new value to store.
   * @param <T> Type of the current and new value
   * @throws GlobalRefDbSystemError the reference cannot be set due to a system error.
   */
  <T> void set(Project.NameKey project, String refName, T newValue) throws GlobalRefDbSystemError;
}
