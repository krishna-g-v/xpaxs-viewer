/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.viewer2d;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.weasis.core.ui.editor.image.QuickViewButtons;

/**
 * The layout and the display mode a viewer opens with, according to the modality of the series it
 * shows.
 *
 * <p>The defaults are described by the {@value #P_DEFAULTS} preference, a comma separated list of
 * {@code <modalities>=<rows>x<columns>[:<mode>]} entries, such as {@code CT=2x6:STACK,
 * US=3x4:TILE,CR+DX=1x2:TILE}. A modality code is a DICOM Modality code; several codes of one entry
 * are joined with {@code +}; the mode is {@code STACK} (a grid of stacks, the default) or {@code
 * TILE} (the grid is the tiles of one series).
 */
public final class ModalityViewDefaults {

  public static final String P_DEFAULTS = "weasis.viewer.modality.defaults"; // NON-NLS
  public static final String DEFAULT_SPEC = "CT=2x6:STACK,US=3x4:TILE,CR+DX=1x2:TILE"; // NON-NLS

  /** Grids offered when choosing a default. */
  public static final List<String> GRIDS =
      List.of(
          "1x1", "1x2", "1x3", "1x4", "2x1", "2x2", "2x3", "2x4", "2x5", "2x6", "3x1", "3x2", "3x3",
          "3x4", "3x5", "3x6", "4x2", "4x3", "4x4"); // NON-NLS

  public enum DisplayMode {
    STACK,
    TILE
  }

  /** The default of one entry: the grid in columns (width) by rows (height), and the mode. */
  public record ViewDefault(Dimension grid, DisplayMode mode) {

    /** Number of series the default can show: one per viewport for a stack, a single one tiled. */
    public int slots() {
      return mode == DisplayMode.TILE ? 1 : grid.width * grid.height;
    }

    public String gridId() {
      return grid.height + "x" + grid.width;
    }

    public String format() {
      return gridId() + ":" + mode;
    }
  }

  private ModalityViewDefaults() {}

  /**
   * Parses a list of entries, by modality codes joined with {@code +}; invalid entries are skipped.
   */
  public static Map<String, ViewDefault> parse(String spec) {
    Map<String, ViewDefault> defaults = new LinkedHashMap<>();
    if (spec != null) {
      for (String entry : spec.split(",")) {
        parseEntry(entry).ifPresent(e -> defaults.put(e.codes(), e.viewDefault()));
      }
    }
    return defaults;
  }

  private record Entry(String codes, ViewDefault viewDefault) {}

  private static Optional<Entry> parseEntry(String entry) {
    String[] sides = entry.split("=");
    if (sides.length != 2) {
      return Optional.empty();
    }
    String codes =
        Arrays.stream(sides[0].split("\\+"))
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .map(code -> code.toUpperCase(Locale.ROOT))
            .collect(Collectors.joining("+"));
    String[] value = sides[1].trim().split(":");
    List<Dimension> grids = QuickViewButtons.parseGrids(value[0]);
    if (codes.isEmpty() || grids.isEmpty() || value.length > 2) {
      return Optional.empty();
    }
    return parseMode(value.length == 2 ? value[1] : null)
        .map(mode -> new Entry(codes, new ViewDefault(grids.getFirst(), mode)));
  }

  private static Optional<DisplayMode> parseMode(String name) {
    if (name == null || name.isBlank()) {
      return Optional.of(DisplayMode.STACK);
    }
    return Arrays.stream(DisplayMode.values())
        .filter(m -> m.name().equalsIgnoreCase(name.trim()))
        .findFirst();
  }

  /** Writes the defaults back as a list of entries. */
  public static String format(Map<String, ViewDefault> defaults) {
    return defaults.entrySet().stream()
        .map(e -> e.getKey() + "=" + e.getValue().format())
        .collect(Collectors.joining(","));
  }

  /** Finds the default of the entry naming the modality. */
  public static Optional<ViewDefault> find(String spec, String modality) {
    if (modality == null) {
      return Optional.empty();
    }
    String code = modality.trim().toUpperCase(Locale.ROOT);
    return parse(spec).entrySet().stream()
        .filter(e -> Arrays.asList(e.getKey().split("\\+")).contains(code))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /** Finds the default to open with, if the given number of series fit in it. */
  public static Optional<ViewDefault> forSeries(String spec, String modality, int seriesCount) {
    return find(spec, modality).filter(viewDefault -> seriesCount <= viewDefault.slots());
  }
}
