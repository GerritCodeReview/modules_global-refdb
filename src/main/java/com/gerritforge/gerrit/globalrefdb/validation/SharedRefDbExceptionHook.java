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

package com.gerritforge.gerrit.globalrefdb.validation;

import com.gerritforge.gerrit.globalrefdb.GlobalRefDbLockException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.server.ExceptionHook;
import java.util.Optional;

public class SharedRefDbExceptionHook implements ExceptionHook {

  @Override
  public boolean shouldRetry(String actionType, String actionName, Throwable throwable) {
    return throwable instanceof GlobalRefDbLockException;
  }

  @Override
  public Optional<Status> getStatus(Throwable throwable) {
    if (throwable instanceof GlobalRefDbLockException) {
      return Optional.of(Status.create(503, "Lock failure"));
    }
    return Optional.empty();
  }

  @Override
  public ImmutableList<String> getUserMessages(Throwable throwable, ImmutableSet<String> traceIds) {
    if (throwable instanceof GlobalRefDbLockException) {
      ImmutableList.Builder<String> builder = new ImmutableList.Builder<>();
      builder.add(throwable.getMessage());
      for (String traceId : traceIds) {
        if (traceId != null && !traceId.isBlank()) {
          builder.add(String.format("Trace ID: %s", traceId));
        }
      }
      return builder.build();
    }
    return ImmutableList.of();
  }
}
