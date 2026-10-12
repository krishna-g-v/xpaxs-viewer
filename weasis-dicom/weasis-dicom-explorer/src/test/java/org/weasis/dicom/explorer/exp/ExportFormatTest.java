/*
 * Copyright (c) 2026 Weasis Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.weasis.dicom.explorer.exp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.weasis.dicom.explorer.exp.LocalExport.Format;

class ExportFormatTest {

  @Test
  void fromName_findsAFormatByNameOrTitleIgnoringCase() {
    assertEquals(Format.DICOM_ZIP, Format.fromName("DICOM_ZIP"));
    assertEquals(Format.DICOM_ZIP, Format.fromName("dicom zip"));
    assertEquals(Format.PNG, Format.fromName("png"));
  }

  @Test
  void fromName_fallsBackToDicom() {
    assertEquals(Format.DICOM, Format.fromName("bogus"));
    assertEquals(Format.DICOM, Format.fromName(null));
  }
}
