/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.core.api.gui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.BoundedRangeModel;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;

/** Scrolling by a page of tiles: 25 images in 4 tiles give the scroll values 1 to 22. */
class SliderPageScrollTest {

  private static final JPanel SOURCE = new JPanel();

  private static SliderChangeListener slider(int value, int step) {
    SliderChangeListener listener =
        new SliderChangeListener(ActionW.SCROLL_SERIES, 1, 22, value, true, 0.1) {
          @Override
          public void stateChanged(BoundedRangeModel model) {
            // value read back from the slider
          }

          @Override
          protected int getScrollStep() {
            return step;
          }
        };
    listener.enableAction(true);
    listener.setButtonMaskEx(InputEvent.BUTTON1_DOWN_MASK);
    return listener;
  }

  private static MouseWheelEvent wheel(int rotation) {
    return new MouseWheelEvent(
        SOURCE,
        MouseEvent.MOUSE_WHEEL,
        0,
        0,
        0,
        0,
        0,
        false,
        MouseWheelEvent.WHEEL_UNIT_SCROLL,
        1,
        rotation);
  }

  private static MouseEvent mouse(int id, int y) {
    return new MouseEvent(SOURCE, id, 0, InputEvent.BUTTON1_DOWN_MASK, 0, y, 1, false);
  }

  @Test
  void wheel_withoutPaging_movesOneValueAtATime() {
    SliderChangeListener listener = slider(5, 1);
    listener.mouseWheelMoved(wheel(1));
    assertEquals(6, listener.getSliderValue());
    listener.mouseWheelMoved(wheel(-2));
    assertEquals(4, listener.getSliderValue());
  }

  @Test
  void wheel_withPaging_movesFromPageStartToPageStart() {
    SliderChangeListener listener = slider(1, 4);
    listener.mouseWheelMoved(wheel(1));
    assertEquals(5, listener.getSliderValue());
    listener.mouseWheelMoved(wheel(1));
    assertEquals(9, listener.getSliderValue());
    listener.mouseWheelMoved(wheel(-1));
    assertEquals(5, listener.getSliderValue());
  }

  @Test
  void wheel_withPaging_realignsAValueInsideAPage() {
    SliderChangeListener listener = slider(7, 4);
    listener.mouseWheelMoved(wheel(1));
    assertEquals(9, listener.getSliderValue());
  }

  @Test
  void wheel_withPaging_reachesTheLastValueThenComesBackAPage() {
    SliderChangeListener listener = slider(21, 4);
    listener.mouseWheelMoved(wheel(1));
    assertEquals(22, listener.getSliderValue(), "the last image must be reachable");
    listener.mouseWheelMoved(wheel(1));
    assertEquals(22, listener.getSliderValue());
    listener.mouseWheelMoved(wheel(-1));
    assertEquals(17, listener.getSliderValue());
  }

  @Test
  void wheel_withPaging_doesNotGoBelowTheFirstValue() {
    SliderChangeListener listener = slider(3, 4);
    listener.mouseWheelMoved(wheel(-1));
    assertEquals(1, listener.getSliderValue());
  }

  @Test
  void drag_withPaging_movesByPagesAndReachesTheLastValue() {
    SliderChangeListener listener = slider(1, 4);
    listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 0));
    // sensitivity 0.1 and 4 values a page: 10 pixels are one page
    listener.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 10));
    assertEquals(5, listener.getSliderValue());
    listener.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 20));
    assertEquals(9, listener.getSliderValue());
    listener.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 500));
    assertEquals(22, listener.getSliderValue());
  }

  @Test
  void drag_withoutPaging_keepsMovingOneValueAtATime() {
    SliderChangeListener listener = slider(1, 1);
    listener.mousePressed(mouse(MouseEvent.MOUSE_PRESSED, 0));
    listener.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED, 20));
    assertEquals(3, listener.getSliderValue());
  }
}
