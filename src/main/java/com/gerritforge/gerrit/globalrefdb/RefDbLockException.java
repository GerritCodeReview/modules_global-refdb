// Copyright (C) 2025 The Android Open Source Project
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

/** {@code RefDbLockException} is an exception that can be thrown when trying to lock a ref. */
public class RefDbLockException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new {@code GlobalRefDbLockException} with the specified project, refName and
   * cause.
   *
   * @param project the project containing refName
   * @param refName the specific ref for which the locking failed
   * @param cause the cause of the locking failure
   */
  public RefDbLockException(String project, String refName, Exception cause) {
    super(String.format("Unable to lock ref %s on project %s", refName, project), cause);
  }

  public RefDbLockException(String project, String refName, String message) {
    super(String.format("Unable to lock ref %s on project %s: %s", refName, project, message));
  }
}
