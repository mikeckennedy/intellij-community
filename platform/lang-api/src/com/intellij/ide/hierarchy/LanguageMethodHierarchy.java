// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.hierarchy;

import com.intellij.lang.LanguageExtension;

/**
 * Provides language-specific implementations of method hierarchy providers.
 */
public class LanguageMethodHierarchy extends LanguageExtension<HierarchyProvider> {
  public static final LanguageMethodHierarchy INSTANCE = new LanguageMethodHierarchy();

  public LanguageMethodHierarchy() {
    super("com.intellij.methodHierarchyProvider");
  }
}
