/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.milton.http;

import io.milton.http.exceptions.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.webdav.DefaultUserAgentHelper;
import java.io.IOException;

public class StandardFilter implements Filter {

	private Logger log = LoggerFactory.getLogger(StandardFilter.class);
	public static final String INTERNAL_SERVER_ERROR_HTML = "<html><body><h1>Internal Server Error (500)</h1></body></html>";

	public StandardFilter() {
	}

	@Override
	public void process(FilterChain chain, Request request, Response response) {
		HttpManager manager = chain.getHttpManager();
		try {
			Request.Method method = request.getMethod();

			Handler handler = manager.getMethodHandler(method);
			if (handler == null) {
				log.warn("No method handler for: " + method + " Please check that dav level 2 protocol support is enabled");
				manager.getResponseHandler().respondMethodNotImplemented(null, response, request);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("delegate to method handler: " + handler.getClass().getCanonicalName());
				}
				handler.process(manager, request, response);
				if (response.getEntity() != null) {
					manager.sendResponseEntity(response);
				} else {
					log.debug("No response entity to send to client for method: " + request.getMethod());
				}

			}

		} catch (BadRequestException ex) {
			log.warn("BadRequestException: " + ex.getReason(), ex);
			manager.getResponseHandler().respondBadRequest(ex.getResource(), response, request);
		} catch (ConflictException ex) {
			log.warn("conflictException: ", ex);
			manager.getResponseHandler().respondConflict(ex.getResource(), response, request, INTERNAL_SERVER_ERROR_HTML);
		} catch (NotAuthorizedException ex) {
			log.warn("NotAuthorizedException", ex);
			manager.getResponseHandler().respondUnauthorised(ex.getResource(), response, request);
		} catch (IOException ex) {
			if (ex.getMessage().contains("Could not get file content")) {
				manager.getResponseHandler().respondConflict(null, response, request, "Could not get file content. Perhaps no replicas are available");
			} else {
				response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_HTML);
			}
		} catch (Throwable e) {
			if (e instanceof NotFoundException) {
				response.sendError(Response.Status.SC_CONFLICT, "Physical resource not found");
			} else {
				// Looks like in some cases we can be left with a connection in an indeterminate state
				// due to the content length not being equal to the content length header, so
				// fall back on the udnerlying connection provider to manage the error
				log.warn("exception sending content", e);
				response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_HTML);
			}
		} finally {
			//manager.closeResponse(response);
		}
	}
}
