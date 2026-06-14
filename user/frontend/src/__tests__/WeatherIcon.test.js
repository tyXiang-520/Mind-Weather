import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import WeatherIcon from '../components/WeatherIcon.vue'

describe('WeatherIcon', () => {
  it('renders sunny icon', () => {
    const wrapper = mount(WeatherIcon, { props: { code: 'sunny', size: 'md' } })
    expect(wrapper.text()).toContain('☀️')
    expect(wrapper.attributes('title')).toBe('晴')
  })

  it('renders cloudy icon', () => {
    const wrapper = mount(WeatherIcon, { props: { code: 'cloudy' } })
    expect(wrapper.text()).toContain('⛅')
  })

  it('renders rainy icon', () => {
    const wrapper = mount(WeatherIcon, { props: { code: 'rainy' } })
    expect(wrapper.text()).toContain('🌧️')
  })

  it('defaults to cloudy for unknown code', () => {
    const wrapper = mount(WeatherIcon, { props: { code: 'unknown' } })
    expect(wrapper.text()).toContain('⛅')
  })

  it('applies size class', () => {
    const wrapper = mount(WeatherIcon, { props: { code: 'sunny', size: 'lg' } })
    expect(wrapper.classes()).toContain('lg')
  })

  it('renders all weather types', () => {
    const types = [
      { code: 'sunny', icon: '☀️' },
      { code: 'cloudy', icon: '⛅' },
      { code: 'overcast', icon: '☁️' },
      { code: 'rainy', icon: '🌧️' },
      { code: 'heavy_rain', icon: '⛈️' },
      { code: 'thunderstorm', icon: '🌩️' }
    ]
    for (const t of types) {
      const wrapper = mount(WeatherIcon, { props: { code: t.code } })
      expect(wrapper.text()).toContain(t.icon)
    }
  })
})
