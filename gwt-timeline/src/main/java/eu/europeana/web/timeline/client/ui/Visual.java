/*
 * Copyright 2007 EDL FOUNDATION
 *
 * Licensed under the EUPL, Version 1.0 or? as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * you may not use this work except in compliance with the
 * Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package eu.europeana.web.timeline.client.ui;

/**
 * For structured initialization of a UI item.
 *
 * @author Serkan Demirel <serkan@blackbuilt.nl>
 */
public interface Visual {

    /**
     * Add the UI elements to the stage
     */
    void buildLayout();

    /**
     * Apply the CSS styles
     */
    void applyStyles();

    /**
     * The width of the element
     *
     * @return The width in pixels
     */
    int getWidth();

    /**
     * The height of the element
     *
     * @return The height in pixels
     */
    int getHeight();

    /**
     * Configure the listeners
     */
    void configureListeners();
}
