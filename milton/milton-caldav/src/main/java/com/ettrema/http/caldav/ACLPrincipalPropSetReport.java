package com.ettrema.http.caldav;

import com.bradmcevoy.http.Resource;
import com.ettrema.http.report.Report;
import org.jdom.Document;

/**
 *
 * @author alex
 */
public class ACLPrincipalPropSetReport implements Report {

	@Override
	public String getName() {
		return "acl-principal-prop-set";
	}

	@Override
	public String process(String host, String path, Resource r, Document doc) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
