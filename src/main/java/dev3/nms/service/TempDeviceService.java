package dev3.nms.service;

import dev3.nms.mapper.TempDeviceMapper;
import dev3.nms.vo.mgmt.TempDeviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TempDeviceService {

    private final TempDeviceMapper tempDeviceMapper;

    public List<TempDeviceVO> findTempDevicesByGroupIds(List<Integer> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return tempDeviceMapper.findTempDevicesByGroupIds(groupIds);
    }

    @Transactional
    public TempDeviceVO createTempDevice(TempDeviceVO device) {
        tempDeviceMapper.insertTempDevice(device);
        return device;
    }

    @Transactional
    public void createTempDevices(List<TempDeviceVO> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }
        tempDeviceMapper.insertTempDevices(devices);
    }

    @Transactional
    public TempDeviceVO updateTempDevice(int tempDeviceId, TempDeviceVO device) {
        device.setTEMP_DEVICE_ID(tempDeviceId);
        tempDeviceMapper.updateTempDevice(device);
        return device;
    }

    @Transactional
    public void deleteTempDevice(int tempDeviceId) {
        tempDeviceMapper.deleteTempDevice(tempDeviceId);
    }

    @Transactional
    public void deleteTempDevices(List<Integer> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        tempDeviceMapper.deleteTempDevices(deviceIds);
    }

    public List<TempDeviceVO> findAllTempDevices() {
        return tempDeviceMapper.findAllTempDevices();
    }
}