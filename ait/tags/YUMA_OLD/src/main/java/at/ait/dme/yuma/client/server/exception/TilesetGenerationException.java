/*
 * Copyright 2008-2010 Austrian Institute of Technology
 *
 * Licensed under the EUPL, Version 1.1 or - as soon they
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

package at.ait.dme.yuma.client.server.exception;

import java.io.Serializable;

public class TilesetGenerationException extends Exception implements Serializable {
	private static final long serialVersionUID = 5195817700909626856L;

	public TilesetGenerationException() {
		
	}
	
	public TilesetGenerationException(String message) {
		super(message);
	}	
	
	public TilesetGenerationException(Throwable cause) {
		super(cause);
	}
	
}
