package com.turkerozturk.richtext.editing;

import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static com.turkerozturk.richtext.editing.PublicWebFetcher.*;

class PublicWebFetcherSecurityTest {
    private InetAddress ip(String value) throws Exception { return InetAddress.getByName(value); }

    @Test void pinsOneCheckedAnswerAndNeverResolvesAgainForTheSocket() throws Exception {
        var lookups = new AtomicInteger();
        InetAddress[] answer = {ip("8.8.8.8")};
        DnsResolver resolver = checkedDns("example.org", host -> {
            if(lookups.incrementAndGet() > 1) throw new AssertionError("DNS rebinding lookup");
            return answer;
        });
        answer[0] = ip("127.0.0.1");
        var first = resolver.resolve("EXAMPLE.ORG");assertThat(first[0].getHostAddress()).isEqualTo("8.8.8.8");
        first[0] = ip("169.254.169.254");
        assertThat(resolver.resolve("example.org")[0].getHostAddress()).isEqualTo("8.8.8.8");
        assertThat(lookups.get()).isEqualTo(1);
        assertThatThrownBy(()->resolver.resolve("other.org")).isInstanceOf(UnknownHostException.class);
        assertThatThrownBy(()->resolver.resolveCanonicalHostname("other.org")).isInstanceOf(UnknownHostException.class);
    }

    @Test void rejectsMixedPublicPrivateAnswersAndEmptyAnswersBeforeConnecting() throws Exception {
        for(var answer:new InetAddress[][]{ {}, {ip("8.8.8.8"),ip("127.0.0.1")}, {ip("2606:4700:4700::1111"),ip("fc00::1")} })
            assertThatThrownBy(()->checkedDns("example.org",host->answer)).isInstanceOf(Problem.class).hasMessage("blocked");
    }

    @Test void redirectsCannotReachPrivateAddressesOrReboundSameHost() throws Exception {
        for(String target:new String[]{"https://private.example/secret","https://public.example/again"}) {
            var lookups = new AtomicInteger();var requests = new AtomicInteger();
            var publicIp=ip("8.8.8.8");var privateIp=ip("127.0.0.1");
            var fetcher=new PublicWebFetcher(host->new InetAddress[]{lookups.incrementAndGet()==1?publicIp:privateIp}) {
                @Override Resource request(URI uri,DnsResolver dns,int limit,long deadline,Budget budget) throws java.io.IOException {
                    requests.incrementAndGet();assertThat(dns.resolve(uri.getHost())[0]).isEqualTo(publicIp);
                    return new Resource(null,URI.create(target),"redirect",null);
                }
            };
            assertThatThrownBy(()->fetcher.fetch("https://public.example/start",100,System.nanoTime()+5_000_000_000L,new Budget()))
                    .isInstanceOf(Problem.class).hasMessage("blocked");
            assertThat(requests.get()).isEqualTo(1);assertThat(lookups.get()).isEqualTo(2);
        }
    }

    @Test void redirectSchemesAndHopLimitRemainEnforced() throws Exception {
        var publicIp=ip("8.8.8.8");var requests=new AtomicInteger();
        var fetcher=new PublicWebFetcher(host->new InetAddress[]{publicIp}) {
            @Override Resource request(URI uri,DnsResolver dns,int limit,long deadline,Budget budget) {
                requests.incrementAndGet();return new Resource(null,uri,"redirect",null);
            }
        };
        assertThatThrownBy(()->fetcher.fetch("https://example.org",100,System.nanoTime()+5_000_000_000L,new Budget()))
                .isInstanceOf(Problem.class).hasMessage("network");
        assertThat(requests.get()).isEqualTo(5);
        var response=new org.apache.hc.core5.http.message.BasicClassicHttpResponse(302);
        response.setHeader("Location","file:///etc/passwd");
        assertThatThrownBy(()->readResponse(response,URI.create("https://example.org"),100,Long.MAX_VALUE,new Budget()))
                .isInstanceOf(Problem.class).hasMessage("url");
    }
}
