import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PostCard from '../components/PostCard.vue'

describe('PostCard', () => {
  const basePost = {
    postId: 1,
    content: '今天天气真好',
    buildingName: '北大楼',
    weatherCode: 'sunny',
    weatherName: '晴',
    weatherIcon: '☀️',
    emotionType: '愉悦',
    tags: ['tag1', 'tag2'],
    createdAt: new Date().toISOString()
  }

  it('renders post content', () => {
    const wrapper = mount(PostCard, { props: { post: basePost } })
    expect(wrapper.text()).toContain('今天天气真好')
    expect(wrapper.text()).toContain('北大楼')
    expect(wrapper.text()).toContain('愉悦')
  })

  it('renders tags', () => {
    const wrapper = mount(PostCard, { props: { post: basePost } })
    expect(wrapper.text()).toContain('tag1')
    expect(wrapper.text()).toContain('tag2')
  })

  it('shows delete button when canDelete', () => {
    const wrapper = mount(PostCard, {
      props: { post: basePost, showDelete: true, canDelete: true }
    })
    expect(wrapper.find('.delete-btn').exists()).toBe(true)
  })

  it('hides delete button when not canDelete', () => {
    const wrapper = mount(PostCard, {
      props: { post: basePost, showDelete: false }
    })
    expect(wrapper.find('.delete-btn').exists()).toBe(false)
  })

  it('shows action bar when showActions', () => {
    const wrapper = mount(PostCard, {
      props: { post: basePost, showActions: true, liked: false, likeCount: 5, commentCount: 3 }
    })
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('3')
  })

  it('shows liked state', () => {
    const wrapper = mount(PostCard, {
      props: { post: basePost, showActions: true, liked: true }
    })
    expect(wrapper.find('.liked').exists()).toBe(true)
  })

  it('emits click on card click', async () => {
    const wrapper = mount(PostCard, { props: { post: basePost } })
    await wrapper.trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('emits delete on delete click', async () => {
    const wrapper = mount(PostCard, {
      props: { post: basePost, showDelete: true, canDelete: true }
    })
    await wrapper.find('.delete-btn').trigger('click')
    expect(wrapper.emitted('delete')).toBeTruthy()
  })
})
