import type { InspirationCard } from '@/types/home'

export const travelTags = ['海边', '小众城市', '历史文化', '美食之旅'] as const

export const inspirationCards: InspirationCard[] = [
  {
    slug: 'xiamen',
    city: '厦门',
    duration: '3天2晚',
    theme: '慢游版',
    description: '吹海风、逛小岛，把日子过得慢一点',
    favorites: '2.4k',
    image: '/images/inspiration-coast.jpg',
  },
  {
    slug: 'chengdu',
    city: '成都',
    duration: '4天3晚',
    theme: '美食打卡',
    description: '在老街和烟火气里，认识一座松弛的城',
    favorites: '1.8k',
    image: '/images/inspiration-city.jpg',
  },
  {
    slug: 'suzhou',
    city: '苏州',
    duration: '3天2晚',
    theme: '园林人文',
    description: '沿着水巷与园林，寻找江南的安静时刻',
    favorites: '3.1k',
    image: '/images/inspiration-nature.jpg',
  },
]
