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

import io.milton.resource.CollectionResource;
import io.milton.resource.LockableResource;
import io.milton.resource.Resource;
import io.milton.common.Path;
import io.milton.http.AuthenticationService.AuthStatus;
import io.milton.http.Request.Method;
import io.milton.http.http11.Http11ResponseHandler;
import io.milton.http.quota.DefaultStorageChecker;
import io.milton.http.quota.StorageChecker;
import io.milton.http.quota.StorageChecker.StorageErrorReason;
import io.milton.common.LogUtils;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.AccessControlledResource;
import io.milton.resource.AccessControlledResource.Priviledge;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class HandlerHelper {

	private final static Logger log = LoggerFactory.getLogger(HandlerHelper.class);
	private AuthenticationService authenticationService;
	private final List<StorageChecker> storageCheckers;
	private boolean enableExpectContinue = true;

	public HandlerHelper(AuthenticationService authenticationService, List<StorageChecker> storageCheckers) {
		this.authenticationService = authenticationService;
		this.storageCheckers = storageCheckers;
	}

	public HandlerHelper(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
		this.storageCheckers = new ArrayList<StorageChecker>();
		storageCheckers.add(new DefaultStorageChecker());
	}

	/**
	 * Checks the expect header, and responds if necessary
	 *
	 * @param resource
	 * @param request
	 * @param response
	 * @return - true if the expect header is ok. ie process normally. false
	 * means that we have sent a CONTINUE status and processing should stop
	 * until the request body is sent
	 */
	public boolean checkExpects(Http11ResponseHandler responseHandler, Request request, Response response) {
		if (enableExpectContinue) {
			String s = request.getExpectHeader();
			LogUtils.trace(log, "checkExpects", s);
			if (s != null && s.length() > 0) {
				response.setStatus(Response.Status.SC_CONTINUE);
				return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	public AuthStatus checkAuthentication(HttpManager manager, Resource resource, Request request) {
		log.trace("checkAuthentication");
		AuthStatus authStatus = authenticationService.authenticate(resource, request); //handler.authenticate( auth.user, auth.password );
		if (authStatus == null) {
			log.trace("checkAuthentication: null authStatus");
			return null;
		} else {
			log.trace("checkAuthentication: authStatus.failed =" + authStatus.loginFailed);
			return authStatus;
		}
	}

	public boolean checkAuthorisation(HttpManager manager, Resource resource, Request request) {
		AuthStatus authStatus = checkAuthentication(manager, resource, request);
		log.trace("checkAuthorisation: " + authStatus);

		// a null authStatus means that no authentication was attempted, eg an anonymous request
		// it is up to the implementation to decide whether or not to allow anonymous access
		// however a failed login must always be rejected
		if (authStatus != null && authStatus.loginFailed) {
			log.trace("checkAuthorisation: loginFailed");
			return false;
		}
		Auth auth;
		if (authStatus != null) {
			log.trace("checkAuthorisation: got auth object");
			auth = authStatus.auth;
		} else {
			log.trace("checkAuthorisation: authStatus is null, no authentication was attempted");
			auth = null;
		}
		return checkAuthorisation(manager, resource, request, request.getMethod(), auth);
	}

	/**
	 * Check that the user has the priviledge to perform the requested operation
	 * on the primary resource, ie that which is specified or implied in the URL
	 *
	 * Note that for a PUT we can
	 *
	 * @param manager
	 * @param resource
	 * @param request
	 * @param auth
	 * @return
	 */
	public boolean checkAuthorisation(HttpManager manager, Resource resource, Request request, Method method, Auth auth) {
//		if (resource instanceof AccessControlledResource) {
//			AccessControlledResource acr = (AccessControlledResource) resource;
//			List<Priviledge> privs = acr.getPriviledges(auth);
//			Priviledge required = findRequiredPriviledge(method, resource);
//		} else {
		boolean authorised = resource.authorise(request, method, auth);
		if (!authorised) {
			if (log.isWarnEnabled()) {
				log.warn("authorisation declined, requesting authentication: " + request.getAbsolutePath() + ". resource type: " + resource.getClass().getCanonicalName());
			}
			if (auth != null) {
				if (log.isTraceEnabled()) {
					log.trace("  - auth: " + auth.getUser() + " tag: " + auth.getTag());
				}
			} else {
				log.trace("  - anonymous request rejected");
			}
			return false;
		} else {
			log.trace("checkAuthorisation: request permitted");
			return true;
		}
//		}
	}

	public boolean doCheckRedirect(Http11ResponseHandler responseHandler, Request request, Response response, Resource resource) throws NotAuthorizedException, BadRequestException {
		String redirectUrl = resource.checkRedirect(request);
		if (redirectUrl != null && redirectUrl.length() > 0) {
			responseHandler.respondRedirect(response, request, redirectUrl);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * TODO: move to webdav
	 *
	 * @param inRequest
	 * @param inResource
	 * @return
	 */
	public boolean isLockedOut(Request inRequest, Resource inResource) {
		if (inResource == null || !(inResource instanceof LockableResource)) {
			return false;
		}
		LockableResource lr = (LockableResource) inResource;
		LockToken token = lr.getCurrentLock();
		if (token != null) {
			Auth auth = inRequest.getAuthorization();
			Object sUser = null;
			if (auth != null) {
				sUser = auth.getUser();
			}
			String lockedByUser = token.info.lockedByUser;
			if (lockedByUser == null) {
				log.warn("Resource is locked with a null user. Ignoring the lock");
				return false;
			} else if (lockedByUser.equals(sUser)) {
//				if (log.isInfoEnabled()) {
//					if (auth == null) {
//						log.trace("lock owned by: " + lockedByUser);
//					} else {
//						log.trace("lock owned by: " + lockedByUser + " not by " + auth.getUser());
//					}
//				}
				String value = inRequest.getIfHeader();
				if (value != null) {
					if (value.contains("opaquelocktoken:" + token.tokenId + ">")) {
						log.trace("Request contains valid token so operation is permitted");
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	public boolean missingLock(Request inRequest, Resource inParentcol) {
		//make sure we are not requiring a lock
		String value = inRequest.getIfHeader();
		if (value != null) {
			if (value.contains("(<DAV:no-lock>)")) {
				log.info("Contained valid token. so is unlocked");
				return true;
			}
		}

		return false;
	}

	public StorageErrorReason checkStorageOnReplace(Request request, CollectionResource parentCol, Resource replaced, String host) {
		for (StorageChecker sc : storageCheckers) {
			StorageErrorReason res = sc.checkStorageOnReplace(request, parentCol, replaced, host);
			if (res != null) {
				log.warn("insufficient storage reason: " + res + " reported by: " + sc.getClass());
				return res;
			}
		}
		return null;
	}

	public StorageErrorReason checkStorageOnAdd(Request request, CollectionResource nearestParent, Path parentPath, String host) {
		for (StorageChecker sc : storageCheckers) {
			StorageErrorReason res = sc.checkStorageOnAdd(request, nearestParent, parentPath, host);
			if (res != null) {
				log.warn("insufficient storage reason: " + res + " reported by: " + sc.getClass());
				return res;
			}
		}
		return null;
	}

	/**
	 * Returns true to indicate that the given resource MUST NOT handle the
	 * given method.
	 *
	 * A return value of false indicates that it might.
	 *
	 * @param r - the resource to check
	 * @param m - the HTTP request method
	 * @return - true to indicate the resource must not handle method m
	 */
	public boolean isNotCompatible(Resource r, Method m) {
		if (r instanceof ConditionalCompatibleResource) {
			ConditionalCompatibleResource ccr = (ConditionalCompatibleResource) r;
			return !ccr.isCompatible(m);
		}
		return false;
	}

	public boolean isEnableExpectContinue() {
		return enableExpectContinue;
	}

	public void setEnableExpectContinue(boolean enableExpectContinue) {
		this.enableExpectContinue = enableExpectContinue;
	}
}
