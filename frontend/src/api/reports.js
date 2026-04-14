import client from './client'

/**
 * 보고서 미리보기 데이터 조회 (JSON)
 * @param {string} type     - grade-summary | student-record | feedback-report | counseling-report
 * @param {Object} params   - { grade, classNum, year, semester }
 */
export const getReportPreview = (type, params = {}) =>
  client.get('/reports/preview', { params: { type, ...params } }).then((r) => r.data.data)

/**
 * 보고서 파일 다운로드 후 브라우저 저장 트리거
 * @param {string} type
 * @param {Object} params  - { grade, classNum, year, semester }
 * @param {string} format  - 'excel' | 'pdf'
 */
export const downloadReport = async (type, params = {}, format = 'excel') => {
  const response = await client.get('/reports/download', {
    params:       { type, format, ...params },
    responseType: 'blob',
  })

  // Content-Disposition 헤더에서 파일명 추출 (없으면 기본값)
  const disposition = response.headers['content-disposition'] || ''
  const match       = disposition.match(/filename="?([^";\n]+)"?/)
  const filename    = match ? match[1] : `report_${type}_${new Date().toISOString().slice(0, 10)}.${format === 'pdf' ? 'pdf' : 'xlsx'}`

  // Blob URL 생성 → <a> 클릭 → 자동 해제
  const url  = URL.createObjectURL(new Blob([response.data]))
  const link = document.createElement('a')
  link.href  = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
