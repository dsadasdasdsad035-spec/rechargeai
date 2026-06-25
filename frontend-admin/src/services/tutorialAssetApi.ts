import http from './http'

export type TutorialAssetKind = 'image' | 'video'

export interface TutorialAssetUploadResult {
  url: string
  storedName: string
  kind: TutorialAssetKind
}

export function uploadTutorialAsset(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<{ data: TutorialAssetUploadResult }>('/tutorial-assets', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: file.type.startsWith('video/') ? 300_000 : 60_000,
  })
}

/** @deprecated 使用 uploadTutorialAsset */
export const uploadTutorialImage = uploadTutorialAsset
