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

package eu.europeana.web.timeline.client.network;

import com.google.gwt.user.client.rpc.AsyncCallback;
import eu.europeana.web.timeline.client.Item;
import eu.europeana.web.timeline.client.ui.Year;

import java.util.List;

/**
 * Asynchronous client-side interface for the JSONService.
 *
 * @author Serkan Demirel <serkan@blackbuilt.nl>
 */
public interface SolrServiceAsync {

    /**
     * Retrieves the years from Solr
     *
     * @param async The callback
     */
    void retrieveYears(AsyncCallback<List<Year>> async);

    /**
     * Retrieve the Items from Solr in java-bin format.
     *
     * @param query  The Solr query
     * @param offset The number of items to skip
     * @param limit  The number of items to fetch
     * @param async  The callback
     */
    void retrieveBriefItems(String query, Integer offset, Integer limit, AsyncCallback<List<Item>> async);
}
