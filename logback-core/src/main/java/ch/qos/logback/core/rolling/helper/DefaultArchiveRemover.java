/**
 * Logback: the generic, reliable, fast and flexible logging framework.
 * 
 * Copyright (C) 2000-2009, QOS.ch
 * 
 * This library is free software, you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation.
 */

package ch.qos.logback.core.rolling.helper;

import java.io.File;
import java.util.Date;

import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.pattern.LiteralConverter;

public class DefaultArchiveRemover implements ArchiveRemover {

  final FileNamePattern fileNamePattern;
  final RollingCalendar rc;
  int maxHistory;
  int periodOffset;
  final boolean parentClean;

  public DefaultArchiveRemover(FileNamePattern fileNamePattern,
      RollingCalendar rc) {
    this.fileNamePattern = fileNamePattern;
    this.rc = rc;
    this.parentClean = computeParentCleaningFlag(fileNamePattern);
  }

  boolean computeParentCleaningFlag(FileNamePattern fileNamePattern) {
    DateTokenConverter dtc = fileNamePattern.getDateTokenConverter();
    // if the date pattern has a /, then we need parent cleaning
    if(dtc.getDatePattern().indexOf('/') != -1) {
      return true;
    }
    // if the literal string subsequent to the dtc contains a /, we also need
    // parent cleaning
    
    Converter<Object> p = fileNamePattern.headTokenConverter;
    
    // find the date converter
    while(p != null) {
      if(p instanceof DateTokenConverter) {
        break;
      }
      p = p.getNext();
    }
    
    while(p != null) {
      if(p instanceof LiteralConverter) {
        String s = p.convert(null);
        if(s.indexOf('/') != -1) {
          return true;
        }
      }
      p = p.getNext();
    }
    
    // no /, so we don't need parent cleaning
    return false;
  }

  public void clean(Date now) {
    Date date2delete = rc.getRelativeDate(now, periodOffset);

    String filename = fileNamePattern.convert(date2delete);

    File file2Delete = new File(filename);

    if (file2Delete.exists() && file2Delete.isFile()) {
      file2Delete.delete();
      if (parentClean) {
        removeFolderIfEmpty(file2Delete.getParentFile(), 0);
      }
    }
  }

  /**
   * Will remove the directory passed as parameter if empty. After that, if the
   * parent is also becomes empty, remove the parent dir as well but at most 3
   * times.
   * 
   * @param dir
   * @param recursivityCount
   */
  void removeFolderIfEmpty(File dir, int recursivityCount) {
    // we should never go more than 3 levels higher
    if (recursivityCount >= 3) {
      return;
    }
    if (dir.isDirectory() && FileFilterUtil.isEmptyDirectory(dir)) {
      dir.delete();
      removeFolderIfEmpty(dir.getParentFile(), recursivityCount + 1);
    }
  }

  public void setMaxHistory(int maxHistory) {
    this.maxHistory = maxHistory;
    this.periodOffset = -maxHistory - 1;
  }

}