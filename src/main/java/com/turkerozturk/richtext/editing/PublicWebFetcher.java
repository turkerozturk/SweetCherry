package com.turkerozturk.richtext.editing;

import java.io.IOException;
import java.net.*;
import java.util.*;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

/** No cookies, credentials, proxies or automatic redirects. Every connection uses checked DNS answers. */
@Component
class PublicWebFetcher {
    static final class Budget {
        private int remaining=20_000_000;
        void take(int bytes){remaining-=bytes;if(remaining<0)throw new Problem("large");}
    }
    record Resource(byte[] bytes, URI uri, String type, String charset) { }
    static final class Problem extends RuntimeException {
        final String code;
        Problem(String code) { super(code); this.code=code; }
    }
    static URI allowedUri(String value) {
        try {
            if(value==null || value.length()>4091 || value.chars().anyMatch(c->c<=32 || c==127))throw new IllegalArgumentException();
            URI uri=URI.create(value);
            if(!Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null
                    || (uri.getPort()!=-1 && uri.getPort()!=(uri.getScheme().equals("https")?443:80)))throw new IllegalArgumentException();
            return URI.create(value.split("#",2)[0]);
        } catch(Exception error) { throw new Problem("url"); }
    }
    /** Rejects private/local, transition and reserved networks, not just RFC1918 addresses. */
    static boolean publicAddress(InetAddress address) {
        if(address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress())return false;
        byte[] b=address.getAddress();int a=b[0]&255,c=b[1]&255,d=b[2]&255;
        if(b.length==4)return a!=0 && a!=10 && a!=127 && a<224
                && !(a==100 && c>=64 && c<=127) && !(a==169 && c==254) && !(a==172 && c>=16 && c<=31)
                && !(a==192 && (c==168 || c==0 && (d==0 || d==2)))
                && !(a==198 && (c==18 || c==19 || c==51 && d==100)) && !(a==203 && c==0 && d==113);
        return (a&224)==32 && !(a==32 && c==2) // IPv6: global unicast only, excluding 6to4.
                && !(a==32 && c==1 && (d<2 || d==13 && (b[3]&255)==184));
    }
    static InetAddress[] checkedAddresses(String host) throws UnknownHostException {
        var addresses=InetAddress.getAllByName(host);
        if(addresses.length==0 || Arrays.stream(addresses).anyMatch(a->!publicAddress(a)))throw new Problem("blocked");
        return addresses;
    }
    @SuppressWarnings("deprecation") // Raw response is necessary: never drain an oversized/redirect body.
    Resource fetch(String value,int limit,long deadline,Budget budget) throws IOException {
        URI uri=allowedUri(value);
        for(int redirects=0;redirects<=4;redirects++) {
            if(System.nanoTime()>deadline)throw new Problem("timeout");
            final String hostname=uri.getHost();
            // Resolve once for this hop. The socket connector receives only these checked addresses;
            // it cannot perform a second DNS lookup after validation (DNS rebinding).
            final InetAddress[] pinned=checkedAddresses(hostname);
            var dns=new DnsResolver() {
                public InetAddress[] resolve(String host) throws UnknownHostException {
                    if(!host.equalsIgnoreCase(hostname))throw new UnknownHostException();return pinned.clone();
                }
                public String resolveCanonicalHostname(String host) { return host; }
            };
            long remaining=Math.max(1,Math.min(8000,(deadline-System.nanoTime())/1_000_000));
            var timeout=Timeout.ofMilliseconds(remaining);
            var manager=PoolingHttpClientConnectionManagerBuilder.create().setDnsResolver(dns)
                    .setDefaultConnectionConfig(ConnectionConfig.custom().setConnectTimeout(timeout).setSocketTimeout(timeout).build()).build();
            try(var client=HttpClients.custom().setConnectionManager(manager).disableRedirectHandling().disableCookieManagement()
                    .disableAuthCaching().disableAutomaticRetries().disableContentCompression()
                    .setDefaultRequestConfig(RequestConfig.custom().setResponseTimeout(timeout).build())
                    .setUserAgent("SweetCherry web-content import").build()) {
                var get=new HttpGet(uri);get.setHeader("Accept","text/html,application/xhtml+xml,image/png,image/jpeg");
                final URI current=uri;
                Resource result;
                try(var response=client.execute(get)) {
                    try { result=readResponse(response,current,limit,deadline,budget); }
                    finally { response.close(org.apache.hc.core5.io.CloseMode.IMMEDIATE); }
                }
                if(!result.type().equals("redirect"))return result;
                uri=result.uri();
            }
        }
        throw new Problem("network");
    }
    static Resource readResponse(org.apache.hc.core5.http.ClassicHttpResponse response,URI current,int limit,long deadline,Budget budget) throws IOException {

        int code=response.getCode();
        if(Set.of(301,302,303,307,308).contains(code)) {
            var location=response.getFirstHeader("Location");
            if(location==null)throw new Problem("network");
            return new Resource(null,allowedUri(current.resolve(location.getValue()).toString()),"redirect",null);
        }
        if(code!=200 || response.getEntity()==null)throw new Problem("network");
        var entity=response.getEntity();
        if(entity.getContentLength()>limit)throw new Problem("large");
        var encoding=response.getFirstHeader("Content-Encoding");
        if(encoding!=null && !encoding.getValue().equalsIgnoreCase("identity"))throw new Problem("type");
        var header=response.getFirstHeader("Content-Type");
        var type=header==null?null:ContentType.parse(header.getValue());
        if(type==null || !Set.of("text/html","application/xhtml+xml","image/png","image/jpeg").contains(type.getMimeType()))throw new Problem("type");
        try(var out=new java.io.ByteArrayOutputStream()) {
            var in=entity.getContent();
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1) {
                if(System.nanoTime()>deadline)throw new Problem("timeout");
                budget.take(n);
                if(out.size()+n>limit)throw new Problem("large");out.write(buffer,0,n);
            }
            return new Resource(out.toByteArray(),current,type.getMimeType(),type.getCharset()==null?null:type.getCharset().name());
        }
    }
}
