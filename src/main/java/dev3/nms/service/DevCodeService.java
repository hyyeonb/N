package dev3.nms.service;

import dev3.nms.mapper.DevCodeMapper;
import dev3.nms.vo.mgmt.DevCodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 장비군(DevCode) 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DevCodeService {

    private final DevCodeMapper devCodeMapper;

    /**
     * 모든 장비군 코드 조회 (플랫 리스트)
     */
    public List<DevCodeVO> getAllDevCodes() {
        return devCodeMapper.findAllDevCodes();
    }

    /**
     * 장비군 코드 트리 구조로 조회
     */
    public List<DevCodeVO> getDevCodeTree() {
        List<DevCodeVO> allCodes = devCodeMapper.findAllDevCodes();
        return buildTree(allCodes);
    }

    /**
     * 플랫 리스트를 트리 구조로 변환
     */
    private List<DevCodeVO> buildTree(List<DevCodeVO> allCodes) {
        Map<Long, DevCodeVO> codeMap = new HashMap<>();
        List<DevCodeVO> roots = new ArrayList<>();

        // 모든 코드를 맵에 등록
        for (DevCodeVO code : allCodes) {
            code.setChildren(new ArrayList<>());
            codeMap.put(code.getDEV_CODE_ID(), code);
        }

        // 부모-자식 관계 설정
        for (DevCodeVO code : allCodes) {
            if (code.getPARENT_DEV_CODE_ID() == null) {
                roots.add(code);
            } else {
                DevCodeVO parent = codeMap.get(code.getPARENT_DEV_CODE_ID());
                if (parent != null) {
                    parent.getChildren().add(code);
                } else {
                    // 부모가 없으면 루트로
                    roots.add(code);
                }
            }
        }

        return roots;
    }

    /**
     * ID로 장비군 코드 조회
     */
    public DevCodeVO getDevCodeById(Long devCodeId) {
        return devCodeMapper.findById(devCodeId).orElse(null);
    }

    /**
     * 장비군 코드 등록
     */
    @Transactional
    public DevCodeVO createDevCode(DevCodeVO devCode) {
        devCodeMapper.insertDevCode(devCode);
        log.info("장비군 코드 등록 완료 - ID: {}, 이름: {}", devCode.getDEV_CODE_ID(), devCode.getCODE_NM());
        return devCode;
    }

    /**
     * 장비군 코드 수정
     */
    @Transactional
    public void updateDevCode(DevCodeVO devCode) {
        devCodeMapper.updateDevCode(devCode);
        log.info("장비군 코드 수정 완료 - ID: {}", devCode.getDEV_CODE_ID());
    }

    /**
     * 장비군 코드 삭제 (하위 코드도 함께 삭제)
     */
    @Transactional
    public void deleteDevCode(Long devCodeId) {
        // 하위 코드 먼저 조회
        List<Long> descendantIds = devCodeMapper.findDevCodeIdWithDescendants(devCodeId);

        // 역순으로 삭제 (자식부터)
        Collections.reverse(descendantIds);
        for (Long id : descendantIds) {
            devCodeMapper.deleteDevCode(id);
        }

        log.info("장비군 코드 삭제 완료 - ID: {}, 삭제된 총 개수: {}", devCodeId, descendantIds.size());
    }
}
