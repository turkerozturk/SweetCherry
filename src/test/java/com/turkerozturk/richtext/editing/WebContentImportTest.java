package com.turkerozturk.richtext.editing;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.ContentType;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.turkerozturk.richtext.editing.PublicWebFetcher.*;

class WebContentImportTest {
    @Test void privateReservedAndTransitionAddressesAreNotFetchable() throws Exception {
        for(String ip:List.of("0.0.0.0","10.0.0.1","127.0.0.1","169.254.169.254","100.64.1.1","172.16.1.1","192.168.1.114","192.0.2.1","198.18.0.1","198.51.100.1","203.0.113.2","224.0.0.1","255.255.255.255","::1","fc00::1","fe80::1","::ffff:127.0.0.1","64:ff9b::7f00:1","2002:7f00:1::","2001:db8::1","2001::1"))
            assertThat(publicAddress(InetAddress.getByName(ip))).as(ip).isFalse();
        for(String ip:List.of("1.1.1.1","8.8.8.8","2606:4700:4700::1111"))assertThat(publicAddress(InetAddress.getByName(ip))).as(ip).isTrue();
        assertThatThrownBy(()->checkedAddresses("127.0.0.1")).isInstanceOf(Problem.class);
    }
    @Test void URLsAreRestrictedWithoutChangingEncodedPathsAndQueries() {
        for(String url:List.of("file:///etc/passwd","ftp://example.org/a","https://user:password@example.org/","http://example.org:8080/","https://example.org:80/","https://example.org/a\nX","https://example.org/ a"))assertThatThrownBy(()->allowedUri(url)).as(url).isInstanceOf(Problem.class);
        assertThat(allowedUri("https://example.org/a%2Fb?q=a%26b#part").toString()).isEqualTo("https://example.org/a%2Fb?q=a%26b");
    }
    @Test void responseLimitsApplyEvenWhenContentLengthIsUnknown() throws Exception {
        var response=new BasicClassicHttpResponse(200);response.setHeader("Content-Type","text/html");
        response.setEntity(new org.apache.hc.core5.http.io.entity.InputStreamEntity(new java.io.ByteArrayInputStream(new byte[200]),-1,ContentType.TEXT_HTML));
        assertThatThrownBy(()->readResponse(response,URI.create("https://example.org"),100,System.nanoTime()+1_000_000_000L,new Budget())).isInstanceOf(Problem.class);
        var redirectResponse=new BasicClassicHttpResponse(302);redirectResponse.setHeader("Location","http://127.0.0.1/private");
        var redirect=readResponse(redirectResponse,URI.create("https://example.org"),100,System.nanoTime()+1_000_000_000L,new Budget());
        assertThat(redirect.type()).isEqualTo("redirect");assertThatThrownBy(()->new PublicWebFetcher().fetch(redirect.uri().toString(),100,System.nanoTime()+1_000_000_000L,new Budget())).isInstanceOf(Problem.class);
    }
    @Test void encodedBodiesAndExpiredRequestsAreRejectedAndBudgetIsShared() throws Exception {
        var response=new BasicClassicHttpResponse(200);response.setHeader("Content-Type","text/html");response.setHeader("Content-Encoding","gzip");response.setEntity(new ByteArrayEntity(new byte[]{1},ContentType.TEXT_HTML));
        assertThatThrownBy(()->readResponse(response,URI.create("https://example.org"),100,System.nanoTime()+1_000_000_000L,new Budget())).isInstanceOf(Problem.class);
        var budget=new Budget();budget.take(19_999_999);assertThatThrownBy(()->budget.take(2)).isInstanceOf(Problem.class);
        assertThatThrownBy(()->new PublicWebFetcher().fetch("https://example.org",100,0,new Budget())).isInstanceOf(Problem.class);
    }
    @Test void staticArticleIncludesNormalizedImagesAndTablesWithoutActiveMarkup() throws Exception {
        var fetcher=mock(PublicWebFetcher.class);var uploads=mock(EmbeddedUploadPolicy.class);when(uploads.fileBytes()).thenReturn(9_000_000);
        String html="<html><head><title>Example</title><base href='http://127.0.0.1/'></head><body><nav>Nav</nav><article><h1 onclick='bad()'>Title</h1><p style='color:red'>Text <a href='../other?q=a%26b'>Link</a></p><img src='img.png' alt='Photo'><img src='http://127.0.0.1/private' alt='Private'><script>bad()</script><iframe src='http://127.0.0.1/'></iframe><table><tr><th>Header</th></tr><tr><td>Value</td></tr></table></article></body></html>";
        when(fetcher.fetch(eq("https://example.org/article"),anyInt(),anyLong(),any())).thenReturn(resource(html,"https://example.org/article","text/html"));
        var image=new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
        when(fetcher.fetch(eq("https://example.org/img.png"),anyInt(),anyLong(),any())).thenReturn(new Resource(out.toByteArray(),URI.create("https://example.org/img.png"),"image/png",null));
        when(fetcher.fetch(eq("http://127.0.0.1/private"),anyInt(),anyLong(),any())).thenThrow(new Problem("blocked"));
        var result=new WebContentImportService(fetcher,uploads).load("https://example.org/article");
        assertThat(result.title()).isEqualTo("Example");assertThat(result.images()).hasSize(1);assertThat(result.skippedImages()).isEqualTo(1);
        assertThat(result.html()).contains("<h1>Title</h1>","color:red","<table>","data-import-image=\"image0\"","https://example.org/other?q=a%26b").doesNotContain("<script","iframe","onclick","<base","src=","Nav");
        assertThat(result.images().values().iterator().next()).startsWith("data:image/png;base64,");
    }
    @Test void invalidImagesAndUntrustedImageTokensAreSkipped() throws Exception {
        var fetcher=mock(PublicWebFetcher.class);var uploads=mock(EmbeddedUploadPolicy.class);when(uploads.fileBytes()).thenReturn(9_000_000);
        when(fetcher.fetch(eq("https://example.org/a"),anyInt(),anyLong(),any())).thenReturn(resource("<img src='bad.png' data-import-image='image0' alt='Broken'>","https://example.org/a","text/html"));
        when(fetcher.fetch(eq("https://example.org/bad.png"),anyInt(),anyLong(),any())).thenReturn(resource("not PNG","https://example.org/bad.png","image/png"));
        var result=new WebContentImportService(fetcher,uploads).load("https://example.org/a");
        assertThat(result.images()).isEmpty();assertThat(result.html()).contains("Broken").doesNotContain("data-import-image");assertThat(result.skippedImages()).isEqualTo(1);
    }
    @Test void duplicateImagesAreFetchedOnceAndOnlyEightOccurrencesAreEmbedded() throws Exception {
        var fetcher=mock(PublicWebFetcher.class);var uploads=mock(EmbeddedUploadPolicy.class);when(uploads.fileBytes()).thenReturn(9_000_000);
        when(fetcher.fetch(eq("https://example.org/a"),anyInt(),anyLong(),any())).thenReturn(resource("<img src='image.png' alt='Photo'>".repeat(12),"https://example.org/a","text/html"));
        var image=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB);var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",out);
        when(fetcher.fetch(eq("https://example.org/image.png"),anyInt(),anyLong(),any())).thenReturn(new Resource(out.toByteArray(),URI.create("https://example.org/image.png"),"image/png",null));
        var result=new WebContentImportService(fetcher,uploads).load("https://example.org/a");
        assertThat(result.images()).hasSize(1);assertThat(org.jsoup.Jsoup.parse(result.html()).select("img")).hasSize(8);assertThat(result.skippedImages()).isEqualTo(4);
        verify(fetcher,times(1)).fetch(eq("https://example.org/image.png"),anyInt(),anyLong(),any());
    }
    @Test void remoteErrorDetailsAreNotReturnedToBrowser() throws Exception {
        var fetcher=mock(PublicWebFetcher.class);when(fetcher.fetch(anyString(),anyInt(),anyLong(),any())).thenThrow(new java.io.IOException("private path or credentials"));
        assertThatThrownBy(()->new WebContentImportService(fetcher,mock(EmbeddedUploadPolicy.class)).load("https://example.org/")).isInstanceOf(Problem.class).hasMessage("network");
    }
    static Resource resource(String body,String url,String type){return new Resource(body.getBytes(StandardCharsets.UTF_8),URI.create(url),type,"UTF-8");}
}
