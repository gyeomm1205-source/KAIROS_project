package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.exception.ActivitySyncFailedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
public class VelogRssService {

    private static final String VELOG_RSS_URL_TEMPLATE = "https://v2.velog.io/rss/%s";
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.ofPattern(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);

    public List<PostInfo> fetchPostsSince(String username, LocalDateTime since) {
        String rssXml = fetchRss(username);
        return parseRss(rssXml, since);
    }

    private String fetchRss(String username) {
        try {
            String response = RestClient.create()
                    .get()
                    .uri(String.format(VELOG_RSS_URL_TEMPLATE, username))
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                throw new ActivitySyncFailedException("Velog", new IllegalStateException("Velog RSS 응답이 비어 있습니다."));
            }
            return response;
        } catch (RestClientException e) {
            throw new ActivitySyncFailedException("Velog", e);
        }
    }

    private List<PostInfo> parseRss(String xml, LocalDateTime since) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)));

            NodeList items = doc.getElementsByTagName("item");
            List<PostInfo> posts = new ArrayList<>();

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String title = getTagText(item, "title");
                String pubDateStr = getTagText(item, "pubDate");

                if (title == null || pubDateStr == null) continue;

                LocalDateTime pubDate = parseRssDate(pubDateStr);
                if (since != null && !pubDate.isAfter(since)) continue;

                posts.add(new PostInfo(title, pubDate));
            }
            return posts;
        } catch (ActivitySyncFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new ActivitySyncFailedException("Velog", e);
        }
    }

    private String getTagText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private LocalDateTime parseRssDate(String dateStr) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(dateStr.trim(), RFC_1123);
            return zdt.withZoneSameInstant(ZoneId.of("Asia/Seoul")).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Velog RSS 날짜 파싱 실패: {}", dateStr);
            return LocalDateTime.MIN;
        }
    }

    public record PostInfo(String title, LocalDateTime publishedAt) {}
}