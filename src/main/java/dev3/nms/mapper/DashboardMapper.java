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

    List<DashboardDto.UserWidgetRes> getUserWidget(Long userId);

    int updateUserWidget(@Param("userId") Long userId, @Param("widget") DashboardDto.UserWidgetReq widget);

    int insertUserWidget(@Param("userId") Long userId, @Param("widget") DashboardDto.UserWidgetReq widget);

    int deleteUserWidget(Map<String, Object> params);

    List<DashboardDto.WidgetPieChartData> getWidgetCpuPieChartData();

    List<DashboardDto.WidgetLineChartData> getWidgetCpuLineChartData();

    List<DashboardDto.WidgetPieChartData> getWidgetMemPieChartData();

    List<DashboardDto.WidgetLineChartData> getWidgetMemLineChartData();

    List<DashboardDto.WidgetBarChartData> getWidgetCpuBarChartData();

    List<DashboardDto.WidgetBarChartData> getWidgetMemBarChartData();

    List<DashboardDto.WidgetBarChartData> getWidgetIcmpBarChartData(Map<String, Object> param);

    List<DashboardDto.WidgetPieChartData> getWidgetIcmpPieChartData(Map<String, Object> param);

    List<DashboardDto.WidgetLineChartData> getWidgetIcmpLineChartData(Map<String, Object> param);

    List<DashboardDto.WidgetBarChartData> getWidgetTrafficBarChartData(Map<String, Object> param);

    List<DashboardDto.WidgetPieChartData> getWidgetTrafficPieChartData(Map<String, Object> param);

    List<DashboardDto.WidgetLineChartData> getWidgetTrafficLineChartData(Map<String, Object> param);
}
