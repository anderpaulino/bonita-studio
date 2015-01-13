/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.bonitasoft.studio.form.preview.i18n;

import org.eclipse.osgi.util.NLS;

/**
 * @Author Aur�lie Zara
 */
public class Messages extends NLS {

    private static String BUNDLE_NAME = "messages";

    public static String previewButton;
    public static String advancedPreviewButton;
    public static String lnfForPreview;
    public static String browserForPreview;

    public static String formPreview;

    public static String noActorDefined;

    public static String noActorMappingDefined;

    public static String noActorDefinedTitle;

    public static String noActorMappingDefinedTitle;

    static {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }
}
