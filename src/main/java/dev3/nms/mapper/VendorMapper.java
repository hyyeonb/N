package dev3.nms.mapper;

import dev3.nms.vo.mgmt.VendorVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VendorMapper {
    VendorVO findVendorByOid(String oid);

    VendorVO findVendorByBaseOid(String baseOid);

    List<VendorVO> findAllVendors();

    List<VendorVO> findAllVendorsUnfiltered();

    VendorVO findVendorById(Integer vendorId);
}
