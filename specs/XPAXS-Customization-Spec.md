# XPAXS Viewer — Customization Specification

| | |
|---|---|
| **Product** | XPAXS viewer (https://xpaxs.com/), a customization of Weasis 4.8 |
| **Repository / branch** | `krishna-g-v/xpaxs-viewer`, `claude/youthful-maxwell-fghb1j` (PR #1 into `master`) |
| **Origin of the requirements** | MedReport360-Viewer (Weasis 3.8.1) of the `mr360-viewer-krishna` and `mr360-viewer-aslam` repositories |
| **Scope** | Only the changes made on top of upstream Weasis 4.8. Upstream behavior is not specified here. |
| **Status of this document** | Describes the implemented behavior. Each requirement is traceable to code, a setting and a test. |

---

## 0. How to read this specification

### 0.1 Conventions

- **Requirement IDs** are `<AREA>-<NN>` (for example `ML-03`) and are stable. Use them in commits, tests and bug reports.
- **MUST / SHOULD / MAY** follow RFC 2119. Every requirement here is implemented and is a MUST unless it says otherwise.
- **Acceptance criteria** use *Given / When / Then* and can be run by hand or turned into a test.
- **Setting** names are keys of the launcher configuration (`weasis-launcher/conf/base.json`, mirrored in `weasis-distributions/etc/config/base.json`). Both files MUST always carry the same new keys. Resolution order at run time: user preferences, then system properties, then `base.json`.
- **Terms**: *stack* = one series in one viewport. *Tile* = the images of one series laid out as a grid in one viewport. *Grid* = `<rows>x<columns>`.

### 0.2 Areas

| Area | Code | Section |
|---|---|---|
| Branding | `BR` | 1 |
| Launcher, protocol and installers | `LA` | 2 |
| Manual-link scroll mode | `ML` | 3 |
| Toolbars and mouse | `TB` | 4 |
| Zoom, scroll and pan behavior | `ZS` | 5 |
| Default display by modality | `MD` | 6 |
| Export defaults | `EX` | 7 |
| Combined X-ray series | `CX` | 8 |

### 0.3 Explicitly out of scope

These parts of MedReport360 were **not** ported, by decision of the product owner: AI annotations, the report panel, the embedded browser and extra tabs, the old application's packaging and auto-updater, and the SmartMed variant.

### 0.4 Design principles that apply to every requirement

1. **Do not disturb upstream behavior by default.** A feature that changes how Weasis 4.8 behaves is either a documented default of this product (listed in section 9) or an option.
2. **Configurable, not hard-coded.** Every default is a setting with a safe fallback when the value is missing or malformed. A bad value MUST NOT prevent the viewer from starting.
3. **Non-destructive.** No feature alters, copies or deletes the user's DICOM data.
4. **Tested.** Logic is placed in small classes with pure methods that unit tests can call without the UI.

---

## 1. Branding (BR)

**Goal.** The viewer presents itself as XPAXS everywhere a user or an installer sees a name or logo, while keeping the upstream licence attribution.

| ID | Requirement |
|---|---|
| BR-01 | The application name shown in the UI (title bars, About box, menus, dialogs) MUST be `XPAXS`. Setting: `weasis.name`, default `XPAXS`. |
| BR-02 | The copyright line MUST read `© <year> XPAXS. Based on Weasis © 2009-<year> Weasis Team`. Setting: `weasis.copyrights`. The upstream attribution MUST stay. |
| BR-03 | The About logo and the loader logo MUST be the XPAXS wordmark, drawn from vector files (`weasis-distributions/resources/svg/logo/WeasisAbout.svg`). They MUST stay sharp at any display scale. File names are kept so upstream code needs no change. |
| BR-04 | The window and title-bar icon MUST be the XPAXS icon (two brackets), vector (`.../logo/Weasis.svg`). |
| BR-05 | The raster images MUST be regenerated from the vector sources: splash `about-round.png` (567 × 244, white rounded card), About picture `about.png` (374 × 147), button `logo-button.png` (64 × 64). |
| BR-06 | Installer icons MUST be the XPAXS icon: Linux PNG (256 px), Windows `.ico` (16, 24, 32, 48, 64, 128, 256 px), macOS `.icns` (16 to 1024 px), snap icon. Images are rendered once large and reduced, so small sizes stay crisp. |
| BR-07 | The Dicomizer and the Image Explorer launchers (`dicomizer.json`, `non-dicom-explorer.json`) keep the Weasis name. This is a known, accepted limit. |

**Acceptance.**
- *Given* a fresh install, *When* the viewer starts, *Then* the splash, the window icon, the title and the About box all show XPAXS and the About box shows the copyright of BR-02.
- *Given* a high-DPI screen, *When* the About box opens, *Then* the logo shows no pixelation.
- *Given* the project tree, *When* searching for `mr360` or `medreport360` (any case), *Then* there is no match.

---

## 2. Launcher, protocol and installers (LA)

### 2.1 Launch protocol

| ID | Requirement |
|---|---|
| LA-01 | The launch link protocol MUST be `xpaxs://` (lower case). The upstream `weasis://` MUST keep working. Pattern: `^(xpaxs\|weasis)(-.*)?://.*?` (`org.weasis.launcher.Utils`). |
| LA-02 | The operating-system URL handler MUST be registered for `xpaxs`: Windows (MSI), macOS (`Info.plist` scheme `xpaxs`, handler `com.xpaxs.url.handler`), Linux (desktop file `XPAXS.desktop`). |
| LA-03 | On macOS the launcher MUST open links through the bundle id `com.xpaxs.launcher` (`WeasisLauncher.MAC_BUNDLE_ID`), the same value as `IDENTIFIER` in the packaging scripts. |
| LA-04 | The temporary directory created for a `xpaxs://` link follows the same clean-up rule as `weasis://` (deleted recursively at startup). |

### 2.2 Profile option `-u`

The old viewer started with `-u <profile>` to give each user their own settings. In 4.8 this is mapped onto the built-in profile support (`weasis.profile`).

| ID | Requirement |
|---|---|
| LA-10 | `-u <name>` MUST select the user profile, whether given as two arguments (`-u alice`) or at the end of the last argument (`xpaxs://... -u alice`). The option is removed from the arguments before they are processed. |
| LA-11 | A value is a profile name only if it is not empty, does not start with `-` or `$`, does not contain `://` and does not start with `file:`. Otherwise `-u` is left untouched, because `-u` is also the URL option of the `image:get` and `dicom:rs` commands. |
| LA-12 | The profile name MUST pass the folder-name validation of the launcher (`addProperties`), as it names a folder under the user's home. An invalid name is rejected, not used. |
| LA-13 | The profile of `-u` MUST apply unless a property already names one. |
| LA-14 | There MUST be one running application instance **per profile**: the single-instance source id is computed from the codebase and the **effective launch profile**, not from the profile written in the configuration file. A second launch with the same profile hands over to the running instance; a launch with another profile starts a new one. |
| LA-15 | The state of the previous launch (the parsed `-u` value) MUST be reset between launches in the same JVM (`ConfigData.clear()`). |

**Acceptance.**
- *Given* `-u alice` then `-u bob`, *Then* two instances run with different source ids and separate preference folders.
- *Given* `dicom:rs -u https://host/wado`, *Then* the profile is not changed and the URL option reaches the command.

### 2.3 Installers and packaging

| ID | Requirement |
|---|---|
| LA-20 | `NAME` is `XPAXS` and `IDENTIFIER` is `com.xpaxs.launcher` in `package-weasis.sh` and in `build-installer.yml`. |
| LA-21 | The vendor MUST be `XPAXS`; the Linux package name `xpaxs`; the DEB maintainer `XPAXS`; the description `XPAXS DICOM viewer`. |
| LA-22 | The Windows upgrade UUIDs (x86-64 and aarch64) MUST differ from Weasis's, so the product installs side by side with Weasis and never upgrades it. |
| LA-23 | The jpackage resource files are keyed by the application name and MUST be named accordingly (`XPAXS.desktop`, `XPAXS.properties`). Linux `prerm`, the MSI `main.wxs` files, the macOS `Info.plist` and the ISO-writer files (`Autorun.inf`, `RUN.bat`) MUST use the new names. |

**Acceptance.** *Given* the installer workflow, *When* run, *Then* the installers are named after XPAXS, install next to a Weasis installation, and `xpaxs://` links open the viewer.

---

## 3. Manual-link scroll mode (ML)

**Background.** Two views can be *manually linked* (for example two CT studies of one patient in a 1 × 2 layout): moving one series moves the other. Weasis 4.8 already provides the link and follows it by **slice location**. MedReport360 followed it **image for image**. Requirement: keep the 4.8 behavior as the default and let the user choose the other.

| ID | Requirement |
|---|---|
| ML-01 | There are two modes (`ManualScrollMode`): `LOCATION` (default) and `INDEX`. An unknown stored name falls back to `LOCATION`. |
| ML-02 | In `LOCATION` mode the linked view MUST behave exactly as in Weasis 4.8: the slice location is shifted by the offset measured at pairing, `location + targetLocation − sourceLocation`, and the nearest image is shown. |
| ML-03 | In `INDEX` mode the linked view MUST keep the image-number offset recorded at pairing: `target = targetIndex + (sourceIndex − pairedSourceIndex)`, clamped to the first and last image of the target series. One image of the source moves the target by one image whatever the slice spacing. |
| ML-04 | When the pairing is made, the index shown by each view MUST be recorded with the location (`ViewSynchData.ManualSyncData` source and target index). A link whose indexes are unknown (`UNKNOWN_INDEX`, −1) MUST fall back to the location rule even in `INDEX` mode. |
| ML-05 | The mode is chosen in the UI: a radio choice (`ManualScrollModeMenu`) in the **Synch** drop-down of the toolbar and in the per-view pop-up. It MUST appear only when the view can be, or is, manually linked. Titles and tooltips come from `messages.properties`. |
| ML-06 | The chosen mode MUST be saved with the DICOM viewer preferences (node `synch`, key `manualScrollMode`) and restored at the next start. |
| ML-07 | The mode is held by `SynchManager` and applies to every manual link of the viewer. |
| ML-08 | The pairing UI (`ManualSynchViewButton`) is unchanged from 4.8. |

**Acceptance.**
- *Given* two CT series of different slice spacing linked at images 10 and 40, *When* the mode is `INDEX` and the user scrolls the source to image 15, *Then* the target shows image 45.
- *Given* the same link, *When* the mode is `LOCATION`, *Then* the target shows the image nearest to the shifted location.
- *Given* the source is scrolled past the end of the target series, *When* the mode is `INDEX`, *Then* the target stays on its last image.
- *Given* the mode is changed and the application restarted, *Then* the mode is the one chosen.

---

## 4. Toolbars and mouse (TB)

### 4.1 One-click layout buttons

| ID | Requirement |
|---|---|
| TB-01 | The main toolbar MUST show one button per grid listed in `weasis.toolbar.layout.quick`, in order. Default: `1x1,1x2,1x3,1x4,2x1,2x2,2x4,2x5,3x1,3x4,3x5,4x2`. Empty hides them. |
| TB-02 | The list is comma separated `<rows>x<columns>`. Invalid entries (not two numbers, zero, 50 or more, extra parts) and duplicates MUST be ignored; the order of the valid ones is kept (`QuickViewButtons.parseGrids`). |
| TB-03 | A button carries the icon and the title of its layout and follows the enabled state of the layout action. |
| TB-04 | A layout button applies the display mode the selected viewport is **in**: if the viewport is stacked, the grid is shown as a grid of stacks; if it is tiled, the grid is shown as the tiles of one series. |

### 4.2 Stack and Tile buttons

| ID | Requirement |
|---|---|
| TB-10 | Two buttons, **Stack** and **Tile**, MUST be shown when `weasis.toolbar.viewmode.buttons` is true (default true). |
| TB-11 | **Tile**, from a multi-view layout, MUST collapse the layout to one viewport and tile it with the current grid. The series of the selected viewport is the one tiled: it is first moved to the first viewport, because a layout change keeps the first viewports. |
| TB-12 | **Tile**, from a single view, MUST tile with a default `2x2` grid and MUST NOT change the layout. |
| TB-13 | **Tile** on an already tiled viewport MUST do nothing. |
| TB-14 | **Stack**, from tiles, MUST switch the viewport back to a stack and restore the tile grid as a grid of stacks. |
| TB-15 | **Stack** on an already stacked viewport MUST do nothing. |
| TB-16 | All these buttons MUST ignore containers that do not support viewport panes. |
| TB-17 | The selected container changes layout through the layout action; any other container is changed directly (`ViewLayoutSwitcher.setLayout`). |

### 4.3 Window/level preset buttons

| ID | Requirement |
|---|---|
| TB-20 | The lookup toolbar MUST show a **W** button (starts the window/level mouse action on the left button) followed by one button per entry of `weasis.toolbar.lut.presets`. Default: `default,weasis.ct.brain,weasis.ct.lung,weasis.ct.mediastinum,weasis.ct.abdomen,weasis.ct.bone`. Empty hides all of them, including **W**. |
| TB-21 | An entry is `default` (the first preset offered for the displayed image) or the id of a configured window preset. Blank and duplicate entries are ignored. |
| TB-22 | A button's label is the preset's name from the preset registry; `default` uses the text `Default`; an unknown id uses the id. |
| TB-23 | A button MUST be enabled only while the displayed image offers the preset, and MUST refresh when the list of offered presets changes. |
| TB-24 | Clicking selects that preset on the preset action. |

### 4.4 Mouse actions

| ID | Requirement |
|---|---|
| TB-30 | Defaults of the mouse buttons: **left = pan, middle = sequence (scroll), right = zoom**. (Upstream: left window/level, middle pan, right context menu.) Settings: `weasis.toolbar.mouse.left`, `.middle`, `.right`. Users can still change them in the UI. |
| TB-31 | A **Save mouse actions** button MUST be shown in the main toolbar when `weasis.toolbar.mouse.save` is true (default true) and more than one mouse button is active. |
| TB-32 | Clicking it MUST write the current mouse mapping to the preferences immediately and **pin** it: when the application quits, the mapping is not saved again, so the mapping the user chose is the one kept (`ImageViewerEventManager.setMouseActionsPinned`). Without a click, the upstream save-on-exit behavior applies. |

### 4.5 External window

| ID | Requirement |
|---|---|
| TB-40 | Moving a viewer to another screen as an external window MUST be offered only when `weasis.viewer.externalize` is true (default true) and the viewer factory allows it. Setting it to false removes the option. |

**Acceptance.**
- *Given* a stacked 1 × 1 viewport, *When* the user clicks `2x2`, *Then* four viewports of stacks appear.
- *Given* a tiled viewport, *When* the user clicks `3x4`, *Then* the same viewport shows 12 tiles (4 columns, 3 rows).
- *Given* a 3 × 4 layout of stacks, *When* the user clicks **Tile**, *Then* one viewport shows the selected series in 12 tiles.
- *Given* a CT series with a lung preset, *Then* the **Lung** button is enabled; *Given* an image without it, *Then* it is disabled.
- *Given* the user clicks **Save mouse actions** then changes the mapping and quits, *Then* the next start restores the saved mapping.

---

## 5. Zoom, scroll and pan behavior (ZS)

### 5.1 Tiled scrolling

In a tiled viewport the scroll value is the **1-based index of the image in the first tile**.

| ID | Requirement |
|---|---|
| ZS-01 | The last scroll value MUST be `max(1, seriesSize − tileCount + 1)` (`ImageViewerEventManager.tiledScrollMax`), so the last image is reachable in the last tile. (Upstream stopped one image short.) |
| ZS-02 | When `weasis.viewer.tile.scroll.page` is true (default), the mouse wheel and the scroll drag MUST move by **a page**: the scroll step is the number of tiles (`getScrollStep`). Otherwise the step is 1. A stack always has step 1. |
| ZS-03 | Paging MUST land on page starts: from any value the wheel first goes back to the start of its page, then moves by whole pages (`SliderChangeListener.steppedValue`). The last value is always reachable even when it is not on a page start. |
| ZS-04 | The scroll drag MUST be scaled by the step and snapped to page starts, except that the slider maximum is reachable. |
| ZS-05 | The same rules apply in the generic viewer and the DICOM viewer (their event managers expose the step to the slider listener). |

### 5.2 Independent modalities (X-ray)

Projection radiographs are not a volume: their images must not be scrolled or zoomed together, even when they share a Frame of Reference UID.

| ID | Requirement |
|---|---|
| ZS-10 | The modalities listed in `weasis.viewer.independent.modalities` (default `CR,DX`; codes are trimmed, case-insensitive; empty for none) are **independent**. |
| ZS-11 | A series of an independent modality MUST have no Frame of Reference for synchronization: `DicomSynchManager.getFrameOfReferenceUID` returns `null`, so its views are never automatically synchronized with others whatever UID the images carry. Manual linking is not affected. |
| ZS-12 | A new image of an independent series MUST be shown **best-fit**, even when the zoom type is "current" (`View2d.resetZoom`, `getZoomRatioFromType`). Other modalities keep the upstream behavior. |

### 5.3 Magnifying lens

| ID | Requirement |
|---|---|
| ZS-20 | **Esc** MUST close the magnifying lens of the selected view first, and update the lens action state; if no lens is open, Esc resets the display as upstream. |

**Acceptance.**
- *Given* a 50-image series in a 2 × 2 tiled viewport, *When* the user scrolls to the end, *Then* the last tile shows image 50 and the scroll value is 47.
- *Given* page scrolling on, *When* the user turns the wheel one notch, *Then* the tiles move by 4 images.
- *Given* two CR series with the same Frame of Reference in two views, *When* the user scrolls one, *Then* the other does not move.
- *Given* a zoomed view, *When* the user opens the next CR image, *Then* it is shown best-fit.
- *Given* the lens is open, *When* Esc is pressed, *Then* only the lens closes.

---

## 6. Default display by modality (MD)

**Goal.** A new DICOM viewer opens in the layout and the mode the radiologist wants for that modality.

### 6.1 Setting

`weasis.viewer.modality.defaults`, default `CT=2x6:STACK,US=3x4:TILE,CR+DX=1x2:TILE`.

- Comma separated entries `<modalities>=<rows>x<columns>[:<mode>]`.
- Modality codes of one entry are joined with `+`; codes are trimmed and upper-cased.
- `<mode>` is `STACK` (default when omitted) or `TILE`, case-insensitive.
- Invalid entries (no `=`, no codes, bad grid, bad mode, extra `:`) MUST be skipped without error. Later entries with the same codes replace earlier ones.

### 6.2 Requirements

| ID | Requirement |
|---|---|
| MD-01 | A *default* is a grid and a mode. `STACK` shows a grid of stacks, one series per viewport; `TILE` shows the grid as the tiles of one series. |
| MD-02 | When a new viewer is created, before its series are added, it MUST apply the default of the modality of the **first** series (`ImageViewerPlugin.applyInitialLayout`, overridden by `View2dContainer`; called from `WeasisWin`). |
| MD-03 | A default MUST apply only when the series to open **fit** in it: the number of cells for `STACK`, one for `TILE` (`ViewDefault.slots`). Otherwise the layout chosen by upstream for the number of series is kept. |
| MD-04 | A viewer that reuses an existing viewer MUST NOT be changed. |
| MD-05 | A modality with no entry keeps the upstream layout. |
| MD-06 | A tiled default collapses the layout to one viewport and tiles it (same mechanism as TB-11). A stack default sets the grid as stacks. |
| MD-07 | Users MUST be able to edit the defaults of **CT**, **US** and **X-ray (CR, DX)** on the DICOM viewer preference page, each with a layout (grid from a fixed list from `1x1` to `4x4`) and a mode (Stack or Tile) (`ModalityViewDefaultsPanel`). Entries the page does not show MUST be kept when saving. |
| MD-08 | The values written back MUST be in the canonical format `<rows>x<columns>:<MODE>`. |

**Acceptance.**
- *Given* the defaults, *When* one CT series is opened, *Then* the viewer shows a 2 × 6 grid of stacks.
- *Given* the defaults, *When* an ultrasound is opened, *Then* one viewport shows it as 12 tiles (3 rows, 4 columns).
- *Given* the defaults, *When* 15 CT series are opened together, *Then* the upstream layout is kept (15 > 12 cells).
- *Given* `CR+DX=1x2:TILE`, *Then* both a CR and a DX series open as two tiles.
- *Given* the preference page, *When* the user changes US to `2x2 / Stack` and saves, *Then* the setting holds `US=2x2:STACK` and the other entries are untouched.

---

## 7. Export defaults (EX)

| ID | Requirement |
|---|---|
| EX-01 | The export dialogs MUST start with the format named by `weasis.export.default.format` (default `DICOM_ZIP`). Accepted: `DICOM`, `DICOM_ZIP`, `JPEG`, `JPEG_XL`, `PNG`, `TIFF`, `JP2` (name or title, case-insensitive). An unknown value gives `DICOM`. The user can still pick another format. |
| EX-02 | When `weasis.export.keyobjects.default` is true (default) and at least one of the series to export has a key object that references an image, the export tree MUST start with these items checked instead of whole studies: the **key images**, the **key objects** and the **annotations** (presentation-state series) of the exported series (`KeyObjectSelection.pathsFor`). |
| EX-03 | If no checked series has a key image, or none of the referenced images is in the tree, the upstream selection (whole studies) MUST be used. |
| EX-04 | The annotations series of a series that is **not** exported MUST NOT be checked. |
| EX-05 | The user can change the checked items freely before exporting. |

**Acceptance.**
- *Given* a study with key images and the export dialog opened from it, *Then* only key images, key objects and annotations are checked and the format is DICOM ZIP.
- *Given* a study without key images, *Then* whole studies are checked as upstream.

---

## 8. Combined X-ray series (CX)

**Background.** Radiographs are often stored one series per exposure, so a radiologist has to open each. The MedReport360 viewer merged them with a fragile download-time patch (global static state, patient merging disabled). This product replaces it with a **display-only series** that cannot damage data.

### 8.1 Requirements

| ID | Requirement |
|---|---|
| CX-01 | The thumbnail context menu of a series MUST offer **Show all X-ray images of the study together** only when exactly one series is selected, its modality is listed in `weasis.viewer.combine.modalities` (default `CR,DX,PX`; empty disables) and its study has **two or more** non-empty series of such modalities. |
| CX-02 | The action MUST build a series of all the images of those series, ordered by **Series Number** (series without a number last), then by the order of each source series; and add it under the same study in the explorer; and open it in the default viewer. |
| CX-03 | The series is named `All X-ray images (<N>)`, `N` being the image count; it has no Series Number; its metadata come from the first image. |
| CX-04 | The combined series MUST share the image objects of its sources. It MUST NOT copy, change or hide any source image or series. |
| CX-05 | There MUST be at most one combined series per study. Its UID is derived from the Study Instance UID (an OID under `2.25.`, name-based UUID), so it is stable. |
| CX-06 | If the action is run again and the combined series already shows exactly the same images, it MUST be reused; otherwise it is replaced by a rebuilt one. |
| CX-07 | **Disposing the combined series MUST NOT dispose its images** (`CombinedProjectionSeries.dispose` drops the references first). This holds when the user removes the combined series, the study or the patient. |
| CX-08 | Removing a **source** series MUST remove the combined series that shares its images, without disposing any image. The remaining sources are untouched. |
| CX-09 | Removing the combined series MUST leave all sources intact. |
| CX-10 | The combined series MUST be excluded from the export tree (`CheckTreeModel`) and from the web manifest (`DicomModelQueryResult`), so no image is exported or counted twice. |
| CX-11 | A combined series MUST NOT itself be a candidate source of another combined series. |

### 8.2 Acceptance

- *Given* a study with CR series 1 (2 images), 2 (3 images) and a CT series, *When* the user right-clicks series 2 and chooses the entry, *Then* a series of 5 images (series 1 first) opens; the CT is not included.
- *Given* the combined series is open, *When* the user removes CR series 1, *Then* the combined view closes and series 2 is still displayable.
- *Given* the user removes the combined series, *Then* both CR series are still in the explorer with all their images.
- *Given* a study with one CR series only, *Then* the entry is not shown.
- *Given* an export of the study, *Then* each image appears once.

---

## 9. Configuration reference

All keys are in `weasis-launcher/conf/base.json` and `weasis-distributions/etc/config/base.json`.

| Key | Default | Type | Req. |
|---|---|---|---|
| `weasis.name` | `XPAXS` | text | BR-01 |
| `weasis.copyrights` | `© ${build.year} XPAXS. Based on Weasis © 2009-${build.year} Weasis Team` | text | BR-02 |
| `weasis.toolbar.mouse.left` | `pan` (upstream `winLevel`) | choice | TB-30 |
| `weasis.toolbar.mouse.middle` | `sequence` (upstream `pan`) | choice | TB-30 |
| `weasis.toolbar.mouse.right` | `zoom` (upstream `contextMenu`) | choice | TB-30 |
| `weasis.toolbar.layout.quick` | `1x1,1x2,1x3,1x4,2x1,2x2,2x4,2x5,3x1,3x4,3x5,4x2` | grid list | TB-01 |
| `weasis.toolbar.viewmode.buttons` | `true` | boolean | TB-10 |
| `weasis.toolbar.mouse.save` | `true` | boolean | TB-31 |
| `weasis.toolbar.lut.presets` | `default,weasis.ct.brain,weasis.ct.lung,weasis.ct.mediastinum,weasis.ct.abdomen,weasis.ct.bone` | id list | TB-20 |
| `weasis.viewer.externalize` | `true` | boolean | TB-40 |
| `weasis.viewer.tile.scroll.page` | `true` | boolean | ZS-02 |
| `weasis.viewer.independent.modalities` | `CR,DX` | code list | ZS-10 |
| `weasis.viewer.combine.modalities` | `CR,DX,PX` | code list | CX-01 |
| `weasis.viewer.modality.defaults` | `CT=2x6:STACK,US=3x4:TILE,CR+DX=1x2:TILE` | entry list | MD-01 |
| `weasis.export.default.format` | `DICOM_ZIP` | choice | EX-01 |
| `weasis.export.keyobjects.default` | `true` | boolean | EX-02 |

Stored user preferences (not in `base.json`): DICOM viewer node `synch`, key `manualScrollMode` = `LOCATION` or `INDEX` (ML-06); the saved mouse mapping (TB-32).

---

## 10. Cross-cutting requirements

| ID | Requirement |
|---|---|
| NF-01 | **Robustness.** Every list or grid setting is parsed leniently (section 6.1, TB-02, TB-21); malformed input is ignored, never fatal. |
| NF-02 | **i18n.** New text lives in the module `messages.properties` (English). Translated bundles are produced by the distribution and are not edited in this repository. |
| NF-03 | **Standards.** No change alters DICOM data, tags or the files of a study. The combined series is a view only. |
| NF-04 | **Licence.** Every source file carries the EPL-2.0 OR Apache-2.0 header and passes Spotless / Google Java Format. |
| NF-05 | **OSGi.** New classes are in exported packages, not in `.internal`; no bundle imports another bundle's `internal` package. |
| NF-06 | **Tests.** JUnit and Mockito only. Tests share no mutable static state (tests run in parallel). Code that needs the UI core is split so its logic is testable through a pure overload. |
| NF-07 | **Documentation.** Concept docs live in `<module>/docs/`: `synch-architecture.md` (ML, ZS), `modality-defaults.md` (MD), `Combined-Projection-Series.md` (CX). |

---

## 11. Traceability

| Area | Main code | Tests |
|---|---|---|
| BR | `weasis-launcher/conf/base.json`, `weasis-distributions/resources/svg/logo/*`, `resources/images/*`, `script/resources/*` | manual (visual) |
| LA-01..04 | `Utils.java`, `WeasisLauncher.java`, `package-weasis.sh`, `Info.plist` | `ConfigDataTest` (protocol cases) |
| LA-10..15 | `ConfigData.java` | `ConfigDataTest` (profile, single-instance cases) |
| LA-20..23 | `package-weasis.sh`, `build-installer.yml`, `script/resources/**` | installer workflow (manual) |
| ML | `ManualScrollMode`, `ManualScrollModeMenu`, `ViewSynchData`, `SynchManager`, `DefaultView2d`, `ViewerToolBar`, dicom `EventManager` | `ManualScrollModeTest`, `ViewSynchDataTest` |
| TB-01..17 | `QuickViewButtons`, `ViewLayoutSwitcher`, `ViewerToolBar` | `QuickViewButtonsTest`, `ViewLayoutSwitcherTest` |
| TB-20..24 | `PresetQuickButtons` | `PresetQuickButtonsTest` |
| TB-30..32 | `base.json`, `ImageViewerEventManager`, `QuickViewButtons` | `QuickViewButtonsTest` |
| TB-40 | `WeasisWin` | manual |
| ZS-01..05 | `ImageViewerEventManager`, `SliderChangeListener` | `TiledScrollRangeTest`, `SliderPageScrollTest` |
| ZS-10..12 | `IndependentModalities`, `DicomSynchManager`, `View2d` | `IndependentModalitiesTest` |
| ZS-20 | `ImageViewerEventManager.escape` | manual |
| MD | `ModalityViewDefaults`, `View2dContainer`, `ModalityViewDefaultsPanel`, `ViewerPrefView`, `ImageViewerPlugin`, `WeasisWin` | `ModalityViewDefaultsTest`, `ModalityViewDefaultsPanelTest` |
| EX | `LocalExport`, `ExportTree`, `KeyObjectSelection` | `ExportFormatTest`, `KeyObjectSelectionTest` |
| CX | `ProjectionSeriesCombiner`, `CombinedProjectionSeries`, `ThumbnailMouseAndKeyAdapter`, `CheckTreeModel`, `DicomModelQueryResult` | `ProjectionSeriesCombinerTest` |

---

## 12. Verification still to do by hand

Automated tests cover the logic. These need a person with real data or an installed build:

1. Two CT studies of one patient linked in a 1 × 2 layout: scroll in `LOCATION` and in `INDEX` mode (ML).
2. A multi-series X-ray study: combined series order, thumbnail, removal of a source, removal of the combined series (CX).
3. Per-modality defaults for CT, US and CR/DX, and the preference page (MD).
4. Tile mode on a large series: drag, wheel, last image reachable (ZS).
5. Installers on Windows, macOS and Linux: names, icons, side-by-side install, `xpaxs://` launch, `-u` profiles (LA, BR).
6. Export dialog with and without key images (EX).
