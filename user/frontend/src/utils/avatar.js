const STORAGE_KEY = 'pickedAvatar'

export const AVATAR_OPTIONS = ['🌤️', '☀️', '⛅', '☁️', '🌧️', '⛈️', '🌩️', '🌈', '🌟', '🌸', '🍀', '🦋']

export function getAvatarUrl() {
  return getPickedIcon()
}

export function getPickedIcon() {
  return localStorage.getItem(STORAGE_KEY) || '🌤️'
}

export function savePickedIcon(icon) {
  localStorage.setItem(STORAGE_KEY, icon)
}
