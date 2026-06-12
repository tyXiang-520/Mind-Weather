package com.mindweather.user.business.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 校园分区配置 —— 与前端 ZoneData.js 保持一致
 * 12 个分区（A-L），104 栋建筑
 */
@Component
public class ZoneConfig {

    private final Map<String, ZoneInfo> zones = new LinkedHashMap<>();
    private final Map<String, String> buildingToZone = new HashMap<>(); // buildingName → zoneId
    private final List<String> allBuildingNames = new ArrayList<>();

    @PostConstruct
    public void init() {
        add("A", "教学核心", "教学楼","新教学楼","图书馆","南教学楼","蒙民伟楼","科技馆","校史博物馆","小礼堂","东南楼","南楼");
        add("B", "历史核心", "北大楼","大礼堂","西大楼","东大楼","辛壬楼","戊己庚楼","丙丁楼","甲乙楼","东北楼","信息管理服务中心");
        add("C", "文科楼群", "逸夫馆","逸夫馆Ⅰ区","逸夫馆Ⅱ区1","逸夫馆Ⅱ区2","逸夫馆Ⅲ区1","逸夫馆Ⅲ区2","逸夫馆Ⅲ区3","费彝民楼","费彝民楼A栋","费彝民楼B栋","田家炳艺术学院","逸夫管理科学楼","南大出版社","建良楼");
        add("D", "运动场馆", "苏浙运动场","吕志和游泳馆");
        add("E", "理科/生活", "西南楼","知行楼","树华楼","声学楼","声学西楼","物理楼","健忠楼","低温实验楼","李四光旧居","罗根泽旧居","赛珍珠故居","创新中心","斗鸡闸","水电管理中心");
        add("F", "餐饮生活", "南园餐厅","教工食堂","教育超市","南园综合楼","南大浴室");
        add("G", "南园宿舍A", "南园1舍","南园2舍","南园3舍","南园4舍","南园5舍","南园6舍","南园7舍","南园17舍","南园18舍","南园19舍","东苑宿舍","校医院","松林楼","中山楼");
        add("H", "陶园/南园B", "综合服务大厅","陶园1舍","陶园2舍","陶园3舍","陶园南楼","南园15舍","南园16舍");
        add("I", "南园宿舍C", "菜鸟驿站","南园13舍","南园14舍","南园20舍","南园21舍","有园宾馆","南园教学楼","荟萃楼","后勤服务集团","校园110报警中心","校园纪念品商店");
        add("J", "南园宿舍D", "南园8舍","南园11舍","南园12舍","外教公寓","拉贝故居","南苑宾馆一号楼","南苑宾馆二号楼");
        add("K", "北区科研", "工程管理学院","天文楼","协鑫楼","平仓楼","平仓楼北楼","华龙楼1号","华龙楼2号","华龙楼3号");
        add("L", "综合/其他", "唐仲英楼","安中楼","曾宪梓楼","实验楼","科学楼","中美文化研究中心","西苑宾馆");
    }

    private void add(String id, String name, String... buildings) {
        zones.put(id, new ZoneInfo(id, name, List.of(buildings)));
        for (String b : buildings) {
            buildingToZone.put(b, id);
            allBuildingNames.add(b);
        }
    }

    public String getZoneId(String buildingName) {
        return buildingToZone.getOrDefault(buildingName, null);
    }

    public ZoneInfo getZone(String zoneId) {
        return zones.get(zoneId);
    }

    public Collection<ZoneInfo> getAllZones() {
        return zones.values();
    }

    public List<String> getAllBuildingNames() {
        return Collections.unmodifiableList(allBuildingNames);
    }

    public boolean isValidBuilding(String name) {
        return buildingToZone.containsKey(name);
    }

    public record ZoneInfo(String id, String name, List<String> buildings) {}
}
