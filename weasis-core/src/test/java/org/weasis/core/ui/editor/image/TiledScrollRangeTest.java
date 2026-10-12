/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.ui.editor.image;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TiledScrollRangeTest {

  @Test
  void lastScrollValue_showsTheLastImageInTheLastTile() {
    // The first tile shows the image at index value - 1, the last tile the one tileCount - 1 later
    int size = 25;
    int tiles = 4;
    int last = ImageViewerEventManager.tiledScrollMax(size, tiles);
    assertEquals(size - 1, (last - 1) + (tiles - 1));
  }

  @Test
  void lastScrollValue_isTheFirstWhenTheTilesCoverTheSeries() {
    assertEquals(1, ImageViewerEventManager.tiledScrollMax(4, 4));
    assertEquals(1, ImageViewerEventManager.tiledScrollMax(3, 4));
    assertEquals(1, ImageViewerEventManager.tiledScrollMax(0, 4));
  }
}
