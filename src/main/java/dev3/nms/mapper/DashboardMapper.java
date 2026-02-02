package dev3.nms.mapper;

import dev3.nms.vo.dashboard.DashboardDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    List<DashboardDto.WidgetsRes> getWidgets();

    List<DashboardDto.DefaultWidgetRes> getDefaultWidget();

    DashboardDto.DefaultWidgetRes getDefaultWidgetById(@Param("widgetId") Long widgetId);

    List<DashboardDto.UserWidgetRes> getUserWidget(Long userId);

    DashboardDto.UserWidgetRes getUserWidgetById(@Param("userDashboardWidgetId") Long userDashboardWidgetId);

    int updateUserWidget(@Param("userId") Long userId, @Param("widget") DashboardDto.UserWidgetReq widget);

    int insertUserWidget(@Param("userId") Long userId, @Param("widget") DashboardDto.UserWidgetReq widget);

    int insertUserWidgetByDefault(@Param("userId") Long userId, @Param("widget") DashboardDto.DefaultWidgetRes widget);

    int deleteUserWidget(Map<String, Object> params);

    List<DashboardDto.WidgetPieChartData> getWidgetCpuPieChartData();

    List<DashboardDto.WidgetLineChartData> getWidgetCpuLineChartData();

    List<DashboardDto.WidgetPieChartData> getWidgetMemPieChartData();

    List<DashboardDto.WidgetLineChartData> getWidgetMemLineChartData();

    List<DashboardDto.WidgetBarChartData> getWidgetCpuBarChartData();

    List<DashboardDto.WidgetBarChartData> getWidgetMemBarChartData();

    List<DashboardDto.WidgetPieChartData> getWidgetCpuMemPieChartData(Map<String, Object> param);

    List<DashboardDto.WidgetLineChartData> getWidgetCpuMemLineChartData(Map<String, Object> param);

    List<DashboardDto.WidgetBarChartData> getWidgetCpuMemBarChartData(Map<String, Object> param);

    List<DashboardDto.WidgetBarChartData> getWidgetIcmpBarChartData(Map<String, Object> param);

    List<DashboardDto.WidgetPieChartData> getWidgetIcmpPieChartData(Map<String, Object> param);

    List<DashboardDto.WidgetLineChartData> getWidgetIcmpLineChartData(Map<String, Object> param);

    List<DashboardDto.WidgetBarChartData> getWidgetTrafficBarChartData(Map<String, Object> param);

    List<DashboardDto.WidgetPieChartData> getWidgetTrafficPieChartData(Map<String, Object> param);

    List<DashboardDto.WidgetLineChartData> getWidgetTrafficLineChartData(Map<String, Object> param);

    DashboardDto.WidgetAlertCntData getWidgetAlertSummary();

    List<DashboardDto.DevCodeData> getDevCode();

    List<Long> getDevCodeAllId(@Param("devCodeId") Long devCodeId);

    Integer getDeviceCountBydevCodeId(List<Long> devCodeIdList);
}
