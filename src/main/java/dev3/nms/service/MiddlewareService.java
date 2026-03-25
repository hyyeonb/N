package dev3.nms.service;

import dev3.nms.mapper.MiddlewareMapper;
import dev3.nms.vo.mgmt.MiddlewareVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiddlewareService {

    private final MiddlewareMapper middlewareMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public List<MiddlewareVO> getAll() {
        return middlewareMapper.findAll();
    }

    public MiddlewareVO getById(Integer id) {
        return middlewareMapper.findById(id);
    }

    public MiddlewareVO getByDeviceId(Integer deviceId) {
        return middlewareMapper.findByDeviceId(deviceId);
    }

    public void create(MiddlewareVO mw) {
        if (mw.getSTATUS() == null) {
            mw.setSTATUS("ACTIVE");
        }
        middlewareMapper.insert(mw);
    }

    public void update(Integer id, MiddlewareVO mw) {
        mw.setMIDDLEWARE_ID(id);
        middlewareMapper.update(mw);
    }

    public void delete(Integer id) {
        int deviceCount = middlewareMapper.countDevicesByMiddlewareId(id);
        if (deviceCount > 0) {
            throw new RuntimeException("해당 미들웨어에 할당된 장비가 " + deviceCount + "개 있습니다. 먼저 장비 할당을 해제하세요.");
        }
        middlewareMapper.delete(id);
    }

    /**
     * 미들웨어 헬스체크 - GET {url}/health 호출 후 LAST_HEARTBEAT 갱신
     */
    public MiddlewareVO healthCheck(Integer id) {
        MiddlewareVO mw = middlewareMapper.findById(id);
        if (mw == null) {
            throw new RuntimeException("미들웨어를 찾을 수 없습니다: " + id);
        }

        try {
            String healthUrl = mw.getMIDDLEWARE_URL() + "/health";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .header("X-API-Key", mw.getAPI_KEY() != null ? mw.getAPI_KEY() : "")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                middlewareMapper.updateHeartbeat(id);
                mw.setSTATUS("ACTIVE");
                log.info("미들웨어 헬스체크 성공 - id: {}, url: {}", id, mw.getMIDDLEWARE_URL());
            } else {
                mw.setSTATUS("DOWN");
                log.warn("미들웨어 헬스체크 실패 - id: {}, status: {}", id, response.statusCode());
            }
        } catch (Exception e) {
            mw.setSTATUS("DOWN");
            log.error("미들웨어 헬스체크 오류 - id: {}, error: {}", id, e.getMessage());
        }

        // 상태 업데이트
        middlewareMapper.update(mw);
        return middlewareMapper.findById(id);
    }
}
