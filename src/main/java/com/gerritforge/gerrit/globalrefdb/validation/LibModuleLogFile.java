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

package com.gerritforge.gerrit.globalrefdb.validation;

import com.google.gerrit.server.util.SystemLog;
import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/** Abstract class to be extended by lib modules willing to create file loggers. */
public abstract class LibModuleLogFile {

  /**
   * Constructs an instance of {@code LibModuleLogFile} which creates an asynchronous appender with
   * the provided logName and layout
   *
   * @param systemLog - to create the log appender
   * @param logName - the name of the log file
   * @param layout - the layout for the log
   * @see org.apache.log4j.PatternLayout
   */
  public LibModuleLogFile(SystemLog systemLog, String logName, Layout layout) {
    Logger logger = LogManager.getLogger(logName);
    if (logger.getAppender(logName) == null) {
      AsyncAppender asyncAppender = systemLog.createAsyncAppender(logName, layout, true, true);
      logger.addAppender(asyncAppender);
      logger.setAdditivity(false);
    }
  }
}
