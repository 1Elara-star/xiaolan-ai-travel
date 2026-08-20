import type { HotelLocationType } from '@/types/travel'

type HotelLocationOptions = Partial<Record<HotelLocationType, string[]>>

const BUSINESS_AREAS: Record<string, string[]> = {
  成都: [
    '春熙路—太古里商圈',
    '交子公园—金融城商圈',
    '天府广场—盐市口商圈',
    '宽窄巷子—人民公园商圈',
    '建设路—东郊记忆商圈',
    '环球中心—世纪城商圈',
  ],
  厦门: [
    '中山路—轮渡商圈',
    '厦大—沙坡尾商圈',
    '曾厝垵—环岛路商圈',
    'SM城市广场商圈',
    '五缘湾商圈',
    '集美学村商圈',
  ],
  苏州: ['观前街商圈', '石路商圈', '金鸡湖商圈', '狮山商圈', '南门商圈', '平江路商圈'],
  南京: [
    '新街口商圈',
    '夫子庙商圈',
    '河西—元通商圈',
    '湖南路—中央路商圈',
    '南京南站—大校场商圈',
    '百家湖商圈',
  ],
  澳门: ['新马路—议事亭前地商圈', '氹仔官也街商圈', '路氹城商圈', '外港商圈'],
}

const LOCATION_OPTIONS: Record<string, HotelLocationOptions> = {
  成都: {
    BUSINESS_AREA: BUSINESS_AREAS.成都,
    TRANSPORT_HUB: ['成都东站', '成都南站', '双流国际机场', '天府国际机场'],
    METRO_STATION: ['春熙路站', '天府广场站', '孵化园站', '火车南站'],
    SCENIC_AREA: ['宽窄巷子', '武侯祠', '成都大熊猫繁育研究基地', '青城山'],
    LANDMARK: ['成都太古里', '成都环球中心', '东郊记忆', '凤凰山体育公园'],
    ADMINISTRATIVE_AREA: ['锦江区', '青羊区', '武侯区', '成华区', '高新区'],
  },
  厦门: {
    BUSINESS_AREA: BUSINESS_AREAS.厦门,
    TRANSPORT_HUB: ['厦门北站', '厦门站', '厦门高崎国际机场', '厦门轮渡码头'],
    METRO_STATION: ['镇海路站', '中山公园站', '吕厝站', '集美学村站'],
    SCENIC_AREA: ['鼓浪屿', '厦门大学', '环岛路', '集美学村'],
    LANDMARK: ['厦门国际会议展览中心', '厦门奥林匹克体育中心', '沙坡尾艺术西区'],
    ADMINISTRATIVE_AREA: ['思明区', '湖里区', '集美区', '海沧区', '翔安区'],
  },
  苏州: {
    BUSINESS_AREA: BUSINESS_AREAS.苏州,
    TRANSPORT_HUB: ['苏州站', '苏州北站', '苏州园区站'],
    METRO_STATION: ['东方之门站', '乐桥站', '察院场站'],
    SCENIC_AREA: ['拙政园', '虎丘山风景名胜区', '金鸡湖景区'],
    LANDMARK: ['苏州中心', '苏州奥林匹克体育中心', '东方之门'],
    ADMINISTRATIVE_AREA: ['姑苏区', '吴中区', '相城区', '虎丘区', '苏州工业园区'],
  },
  南京: {
    BUSINESS_AREA: BUSINESS_AREAS.南京,
    TRANSPORT_HUB: ['南京南站', '南京站', '南京禄口国际机场'],
    METRO_STATION: ['新街口站', '夫子庙站', '元通站'],
    SCENIC_AREA: ['中山陵', '夫子庙秦淮风光带', '玄武湖公园'],
    LANDMARK: ['南京奥体中心', '南京国际博览中心', '南京博物院'],
    ADMINISTRATIVE_AREA: ['玄武区', '秦淮区', '建邺区', '鼓楼区', '雨花台区'],
  },
  澳门: {
    BUSINESS_AREA: BUSINESS_AREAS.澳门,
    TRANSPORT_HUB: ['澳门国际机场', '港珠澳大桥澳门口岸', '澳门外港客运码头'],
    METRO_STATION: ['路氹东站', '莲花站', '排角站'],
    SCENIC_AREA: ['大三巴牌坊', '澳门旅游塔', '官也街'],
    LANDMARK: ['澳门威尼斯人', '澳门银河综艺馆', '新濠影汇综艺馆'],
    ADMINISTRATIVE_AREA: ['澳门半岛', '氹仔', '路环', '路氹城'],
  },
}

/** 根据当前目的地返回不同住宿位置类型的常用选项，列表外仍允许自行输入。 */
export function getHotelLocationOptions(destination?: string | null): HotelLocationOptions {
  const normalized = destination?.trim()
  if (!normalized) return {}

  const city = Object.keys(LOCATION_OPTIONS).find((name) => normalized.includes(name))
  return city ? (LOCATION_OPTIONS[city] ?? {}) : {}
}
