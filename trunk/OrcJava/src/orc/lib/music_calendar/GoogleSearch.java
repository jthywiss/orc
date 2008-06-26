package orc.lib.music_calendar;

import javax.xml.rpc.ServiceException;

import GoogleSearch.GoogleSearchServiceLocator;
import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.EvalSite;
import orc.runtime.sites.java.ThreadedObjectProxy;
import orc.runtime.values.Value;

/**
 * A hack to work around the fact that dynamic web service
 * loading doesn't yet work in servlets.
 * 
 * <p>FIXME: delete this once Webservice works reliably in
 * a servlet environment.
 * 
 * @author quark
 */
public class GoogleSearch extends ThreadedObjectProxy {
	public GoogleSearch() throws ServiceException {
		super(new GoogleSearchServiceLocator().getGoogleSearchPort());
	}
}