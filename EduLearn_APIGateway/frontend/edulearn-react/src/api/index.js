// ═══════════════════════════════════════════════════════════════
//  EduLearn API Layer — all requests route through the API Gateway
//  Gateway: http://localhost:8080
//  JWT is read from localStorage and injected via interceptor.
// ═══════════════════════════════════════════════════════════════
import axios from 'axios'

const TOKEN_KEY = 'edulearn_jwt'

export const getToken      = ()  => localStorage.getItem(TOKEN_KEY)
export const setToken      = (t) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken    = ()  => localStorage.removeItem(TOKEN_KEY)
export const getUserFromToken = () => {
  const t = getToken()
  if (!t) return null
  try {
    const payload = JSON.parse(atob(t.split('.')[1]))
    return { userId: payload.userId, email: payload.sub, role: payload.role }
  } catch { return null }
}

// ── Single gateway client ────────────────────────────────────────────────────
const GATEWAY_BASE = 'http://localhost:8080/api/v1'

const makeClient = (baseURL) => {
  const c = axios.create({ baseURL, timeout: 15000 })
  c.interceptors.request.use(cfg => {
    const t = getToken()
    if (t) cfg.headers.Authorization = `Bearer ${t}`
    return cfg
  })
  c.interceptors.response.use(
    r => r.data,
    err => {
      if (err.response?.status === 401) { clearToken(); window.location.href = '/login' }
      return Promise.reject(err.response?.data || err.message)
    }
  )
  return c
}

const api = makeClient(GATEWAY_BASE)

// Named clients kept for backward compatibility (all point to gateway)
export const authAPI         = api
export const courseAPI       = api
export const lessonAPI       = api
export const enrollmentAPI   = api
export const paymentAPI      = api
export const progressAPI     = api
export const assessmentAPI   = api
export const discussionAPI   = api
export const notificationAPI = api

// Auth Service  →  /api/v1/auth/**
export const auth = {
  register      : (data)        => api.post('/auth/register', data),
  login         : (data)        => api.post('/auth/login', data),
  getProfile    : ()            => api.get('/auth/profile'),
  updateProfile : (data)        => api.put('/auth/profile', data),
  changePassword: (data)        => api.put('/auth/password', data),
  validateToken : (token)       => api.get('/auth/validate', { headers: { Authorization: `Bearer ${token}` } }),
  getUsersByRole: (role)        => api.get(`/auth/users/role/${role}`),
  searchUsers   : (name)        => api.get(`/auth/users/search?name=${name}`),
  deleteUser    : (id)          => api.delete(`/auth/admin/users/${id}`),
}

// Course Service  →  /api/v1/courses/**
export const courses = {
  getAll        : (params)      => api.get('/courses', { params }),
  search        : (params)      => api.get('/courses/search', { params }),
  featured      : ()            => api.get('/courses/featured'),
  top           : (limit = 10)  => api.get(`/courses/top?limit=${limit}`),
  free          : ()            => api.get('/courses/free'),
  byId          : (id)          => api.get(`/courses/${id}`),
  byCategory    : (cat, params) => api.get(`/courses/category/${cat}`, { params }),
  byInstructor  : (id)          => api.get(`/courses/instructor/${id}`),
  create        : (data)        => api.post('/courses', data),
  update        : (id, data)    => api.put(`/courses/${id}`, data),
  publish       : (id, published) => api.put(`/courses/${id}/publish`, { published }),
  delete        : (id)          => api.delete(`/courses/${id}`),
  toggleFeatured: (id, f)       => api.put(`/courses/${id}/featured?featured=${f}`),
}

// Lesson Service  →  /api/v1/lessons/**
export const lessons = {
  byCourse      : (cid)         => api.get(`/lessons/course/${cid}`),
  previews      : (cid)         => api.get(`/lessons/course/${cid}/preview`),
  byId          : (id)          => api.get(`/lessons/${id}`),
  add           : (cid, data)   => api.post(`/lessons/course/${cid}`, data),
  update        : (id, data)    => api.put(`/lessons/${id}`, data),
  delete        : (id)          => api.delete(`/lessons/${id}`),
  reorder       : (cid, data)   => api.put(`/lessons/course/${cid}/reorder`, data),
  addResource   : (id, data)    => api.post(`/lessons/${id}/resources`, data),
  removeResource: (lid, rid)    => api.delete(`/lessons/${lid}/resources/${rid}`),
  resources     : (id)          => api.get(`/lessons/${id}/resources`),
}

// Enrollment Service  →  /api/v1/enrollments/**
export const enrollments = {
  enroll        : (courseId)    => api.post('/enrollments', { courseId }),
  unenroll      : (courseId)    => api.delete(`/enrollments/${courseId}`),
  mine          : ()            => api.get('/enrollments/my'),
  byCourse      : (cid)         => api.get(`/enrollments/course/${cid}`),
  check         : (sid, cid)    => api.get(`/enrollments/check?studentId=${sid}&courseId=${cid}`),
  updateProgress: (cid, pct)    => api.put(`/enrollments/${cid}/progress`, { progressPercent: pct }),
  complete      : (cid)         => api.put(`/enrollments/${cid}/complete`),
  issueCert     : (cid)         => api.post(`/enrollments/${cid}/certificate`),
  count         : (cid)         => api.get(`/enrollments/course/${cid}/count`),
  stats         : (cid)         => api.get(`/enrollments/course/${cid}/stats`),
}

// Payment Service  →  /api/v1/payments/**
export const payments = {
  purchase      : (data)        => api.post('/payments', data),
  myPayments    : ()            => api.get('/payments/my'),
  refund        : (id)          => api.post(`/payments/${id}/refund`),
  subscribe     : (data)        => api.post('/payments/subscriptions', data),
  mySubscription: ()            => api.get('/payments/subscriptions/my'),
  cancelSub     : ()            => api.delete('/payments/subscriptions/my'),
  renewSub      : ()            => api.post('/payments/subscriptions/my/renew'),
  isSubActive   : (sid)         => api.get(`/payments/subscriptions/active?studentId=${sid}`),
  revenue       : ()            => api.get('/payments/admin/revenue'),
}

// Progress / Certificate Service  →  /api/v1/progress/**, /api/v1/certificates/**
export const progress = {
  track         : (data)        => api.post('/progress', data),
  markComplete  : (lid, cid)    => api.put(`/progress/lesson/${lid}/complete?courseId=${cid}`),
  forLesson     : (lid)         => api.get(`/progress/lesson/${lid}`),
  forCourse     : (cid, total)  => api.get(`/progress/course/${cid}?totalLessons=${total || 0}`),
  mine          : ()            => api.get('/progress/my'),
  issueCert     : (data)        => api.post('/certificates', data),
  myCerts       : ()            => api.get('/certificates/my'),
  certForCourse : (cid)         => api.get(`/certificates/course/${cid}`),
  verifyCert    : (code)        => api.get(`/certificates/verify/${code}`),
}

// Assessment Service  →  /api/v1/quizzes/**, /api/v1/questions/**, /api/v1/attempts/**
export const assessment = {
  createQuiz    : (data)        => api.post('/quizzes', data),
  quizById      : (id)          => api.get(`/quizzes/${id}`),
  byCourse      : (cid)         => api.get(`/quizzes/course/${cid}`),
  updateQuiz    : (id, data)    => api.put(`/quizzes/${id}`, data),
  publishQuiz   : (id, p)       => api.put(`/quizzes/${id}/publish?publish=${p}`),
  deleteQuiz    : (id)          => api.delete(`/quizzes/${id}`),
  addQuestion   : (qid, data)   => api.post(`/quizzes/${qid}/questions`, data),
  getQuestions  : (qid)         => api.get(`/quizzes/${qid}/questions`),
  updateQ       : (id, data)    => api.put(`/questions/${id}`, data),
  deleteQ       : (id)          => api.delete(`/questions/${id}`),
  startAttempt  : (qid)         => api.post(`/quizzes/${qid}/start`),
  submit        : (data)        => api.post('/attempts/submit', data),
  myAttempts    : ()            => api.get('/attempts/my'),
  quizAttempts  : (qid)         => api.get(`/quizzes/${qid}/attempts`),
  bestScore     : (qid)         => api.get(`/quizzes/${qid}/best-score`),
}

// Discussion Service  →  /api/v1/threads/**, /api/v1/replies/**
export const discussion = {
  createThread  : (data)        => api.post('/threads', data),
  getThread     : (id)          => api.get(`/threads/${id}`),
  byCourse      : (cid)         => api.get(`/threads/course/${cid}`),
  byLesson      : (lid)         => api.get(`/threads/lesson/${lid}`),
  search        : (cid, kw)     => api.get(`/threads/course/${cid}/search?keyword=${kw}`),
  deleteThread  : (id)          => api.delete(`/threads/${id}`),
  pin           : (id, p)       => api.put(`/threads/${id}/pin?pin=${p}`),
  close         : (id, c)       => api.put(`/threads/${id}/close?close=${c}`),
  postReply     : (tid, data)   => api.post(`/threads/${tid}/replies`, data),
  getReplies    : (tid)         => api.get(`/threads/${tid}/replies`),
  upvote        : (rid)         => api.post(`/replies/${rid}/upvote`),
  accept        : (rid)         => api.put(`/replies/${rid}/accept`),
  deleteReply   : (rid)         => api.delete(`/replies/${rid}`),
}

// Notification Service  →  /api/v1/notifications/**
export const notifications = {
  mine        : (page = 0, size = 20) => api.get(`/notifications/my?page=${page}&size=${size}`),
  unreadCount : ()                    => api.get('/notifications/my/unread-count'),
  markRead    : (id)                  => api.put(`/notifications/${id}/read`),
  markAllRead : ()                    => api.put('/notifications/my/read-all'),
  delete      : (id)                  => api.delete(`/notifications/${id}`),
  sendBulk    : (data)                => api.post('/notifications/bulk', data),
  adminInbox  : (uid, p = 0, s = 20) => api.get(`/notifications/admin/user/${uid}?page=${p}&size=${s}`),
}
