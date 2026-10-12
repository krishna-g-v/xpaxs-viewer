# Default display by modality

How a new 2D viewer chooses its layout and display mode from the modality of the series it opens.

## Concept

A viewer shows its series either as a **grid of stacks**, one series per viewport (`STACK`), or as
the **tiles of one viewport**, the grid being the images of a single series (`TILE`). The default
of a modality is a grid, `<rows>x<columns>`, and one of these modes.

The defaults are the `weasis.viewer.modality.defaults` preference, a comma separated list of
`<modalities>=<rows>x<columns>[:<mode>]` entries, where the modality codes of an entry are joined
with `+`. Users edit CT, US and X-ray (CR and DX) on the DICOM viewer preference page; the entries
that page does not show are kept.

## Flow

`WeasisWin` calls `ImageViewerPlugin.applyInitialLayout` on a newly created viewer, before its
series are added. `View2dContainer` overrides it: the modality of the first series selects an
entry of `ModalityViewDefaults`, which is applied with `ViewLayoutSwitcher` — the class that also
serves the Stack, Tile and layout buttons of `QuickViewButtons`.

- A default applies only when the series fit in it (`ViewDefault.slots`: the number of cells of a
  stack grid, one for tiles); otherwise the layout chosen for the number of series is kept.
- Viewers that reuse an existing one are not changed.
- A tiled default collapses the layout to one viewport and tiles it with the grid; the series to
  show is moved to the first viewport first, because a layout change keeps the first viewports.
- The container that is not the selected one has its layout changed directly instead of through
  the layout action, which follows the selected container.
