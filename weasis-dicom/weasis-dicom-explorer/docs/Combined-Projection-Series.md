# Combined projection series

Projection radiographs (CR, DX, PX) are often stored one series per exposure. The thumbnail context
menu of such a series offers to show all the radiographs of its study together, in one viewer.

## Concept

- `ProjectionSeriesCombiner` gathers the series of the study whose Modality is listed by the
  `weasis.viewer.combine.modalities` preference (default `CR,DX,PX`) and builds a
  `CombinedProjectionSeries`. The entry is offered when at least two series qualify.
- Images are ordered by Series Number (series without number last), then in the order of each
  source series.
- The combined series is **display only**: it shares the `DicomImageElement` instances of its
  sources and never copies or alters them. Its UID is derived from the Study Instance UID, so a
  study has at most one combined series, which is rebuilt when the sources have changed.

## Invariants

- `CombinedProjectionSeries.dispose()` only drops its references: the images belong to the source
  series and are disposed with them.
- Removing a source series removes the combined series of the study without disposing any image;
  removing the combined series leaves the sources intact.
- The combined series is skipped by the export tree and by the web manifest (`CheckTreeModel`,
  `DicomModelQueryResult`), so no image is exported twice.
