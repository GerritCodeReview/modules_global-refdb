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

import static com.google.common.truth.Truth.assertThat;

import java.util.Optional;
import org.junit.Test;

public class ExtendedGlobalRefDatabaseTest extends GlobalRefDatabaseTest {

  @Test
  public void shouldSetLongValueInTheGlobalRefDB() {
    objectUnderTest.set(project, refName, 1L);

    Optional<Long> o = objectUnderTest.get(project, refName, Long.class);

    assertThat(o.isPresent()).isTrue();
    assertThat(o.get()).isEqualTo(1L);
  }
}
