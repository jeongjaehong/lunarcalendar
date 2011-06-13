package org.nilriri.LunaCalendar.gcal;

import java.io.IOException;

import com.google.api.client.googleapis.GoogleUrl;
import com.google.api.client.http.HttpExecuteIntercepter;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;

public class RedirectHandler {

    /**
     * See <a
     * href="http://code.google.com/apis/calendar/faq.html#redirect_handling">How
     * do I handle redirects...?</a>.
     */
    private static class SessionIntercepter implements HttpExecuteIntercepter {

        private String gsessionid;

        private SessionIntercepter(HttpTransport transport, GoogleUrl locationUrl) {
            this.gsessionid = (String) locationUrl.getFirst("gsessionid");
            transport.removeIntercepters(SessionIntercepter.class);
            transport.intercepters.add(0, this); // must be first
        }

        public void intercept(HttpRequest request) {
            request.url.set("gsessionid", this.gsessionid);
        }
    }

    /** Resets the session ID stored for the HTTP transport. */
    public static void resetSessionId(HttpTransport transport) {
        transport.removeIntercepters(SessionIntercepter.class);
    }

    static HttpResponse execute(HttpRequest request) throws IOException {
        try {
            return request.execute();
        } catch (HttpResponseException e) {
            if (e.response.statusCode == 302) {
                GoogleUrl url = new GoogleUrl(e.response.headers.location);
                request.url = url;
                new SessionIntercepter(request.transport, url);
                e.response.ignore();
                try {
                    HttpResponse res = request.execute();
                    return res;
                } catch (HttpResponseException re) {

                    if (re.response.statusCode == 400 || re.response.statusCode == 403 || re.response.statusCode == 412) {
                        return re.response;
                    } else {
                        re.printStackTrace();
                        throw re;
                    }
                }
            } else {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
