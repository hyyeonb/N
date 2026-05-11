package dev3.nms.controller;

import dev3.nms.mapper.MiddlewareMapper;
import dev3.nms.vo.mgmt.MiddlewareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 관제 SSE 스트림 집계 컨트롤러
 * - 모든 ACTIVE 미들웨어의 /api/watch/stream/{groupId} 를 병렬 구독
 * - 수신 이벤트를 단일 SseEmitter 로 머지하여 브라우저에 전달
 * - 장비가 여러 미들웨어에 분산된 경우에도 한 연결로 전체 메트릭 수신
 */
@Slf4j
@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchStreamController {

    private final MiddlewareMapper middlewareMapper;

    @GetMapping(path = "/stream/{groupId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Integer groupId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        AtomicBoolean alive = new AtomicBoolean(true);

        List<MiddlewareVO> actives = middlewareMapper.findActiveMiddlewares();
        if (actives == null || actives.isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data("{\"message\":\"활성 미들웨어가 없습니다\"}"));
            } catch (Exception ignore) {}
            emitter.complete();
            return emitter;
        }

        ExecutorService pool = Executors.newFixedThreadPool(actives.size(),
                r -> {
                    Thread t = new Thread(r, "sse-proxy-" + groupId);
                    t.setDaemon(true);
                    return t;
                });

        Runnable shutdown = () -> {
            alive.set(false);
            pool.shutdownNow();
        };
        emitter.onCompletion(shutdown);
        emitter.onTimeout(shutdown);
        emitter.onError(e -> shutdown.run());

        log.info("[SSE] 집계 연결 시작 - GroupID: {}, 미들웨어 {}개", groupId, actives.size());

        for (MiddlewareVO mw : actives) {
            pool.submit(() -> proxyStream(mw, groupId, emitter, alive));
        }

        return emitter;
    }

    private void proxyStream(MiddlewareVO mw, Integer groupId, SseEmitter emitter, AtomicBoolean alive) {
        String mwUrl = mw.getMIDDLEWARE_URL();
        if (mwUrl == null || mwUrl.isEmpty()) return;

        String url = mwUrl.replaceAll("/+$", "") + "/api/watch/stream/" + groupId;
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "text/event-stream")
                    .GET()
                    .build();

            HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (resp.statusCode() != 200) {
                log.warn("[SSE Proxy] 비정상 응답 - mwId: {}, status: {}", mw.getMIDDLEWARE_ID(), resp.statusCode());
                return;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                String line;
                while (alive.get() && (line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String payload = line.substring(6).trim();
                        if (payload.isEmpty()) continue;
                        synchronized (emitter) {
                            try {
                                emitter.send(SseEmitter.event().data(payload));
                            } catch (Exception e) {
                                alive.set(false);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (alive.get()) {
                log.warn("[SSE Proxy] 미들웨어 SSE 실패 - mwId: {}, url: {}, err: {}",
                        mw.getMIDDLEWARE_ID(), url, e.getMessage());
            }
        }
    }
}
