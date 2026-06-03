import { useState, useEffect, useRef, useCallback } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import CartFloatingButton from '../components/CartFloatingButton'

const CATEGORIES = ['전체', '한식', '중식', '치킨', '피자', '분식', '일식']

const CATEGORY_EMOJI = {
  '한식': '🍚', '중식': '🥢', '치킨': '🍗',
  '피자': '🍕', '분식': '🍢', '일식': '🍣',
}

// 강남역 기본 좌표
const DEFAULT_LAT = 37.4979
const DEFAULT_LNG = 127.0276

function getSessionId() {
  let sid = localStorage.getItem('sessionId')
  if (!sid) {
    sid = 'session-' + Math.random().toString(36).substring(2, 10)
    localStorage.setItem('sessionId', sid)
  }
  return sid
}

export { getSessionId }

export default function ShopListPage() {
  const [shops, setShops] = useState([])
  const [category, setCategory] = useState('전체')
  const [loading, setLoading] = useState(false)
  const [initialLoading, setInitialLoading] = useState(true)
  const [location] = useState({ lat: DEFAULT_LAT, lng: DEFAULT_LNG })
  const [hasNext, setHasNext] = useState(false)
  const [page, setPage] = useState(0)

  const observerRef = useRef(null)
  const loadingRef = useRef(false)

  const fetchPage = useCallback((nextPage, isInitial) => {
    if (loadingRef.current) return
    loadingRef.current = true
    setLoading(true)

    const params = { lat: location.lat, lng: location.lng, page: nextPage }
    if (category !== '전체') params.category = category

    client.get('/shops', { params })
      .then(res => {
        const data = res.data
        const items = Array.isArray(data?.content) ? data.content : []
        setShops(prev => isInitial ? items : [...prev, ...items])
        setHasNext(Boolean(data?.hasNext))
        setPage(nextPage)
      })
      .catch(console.error)
      .finally(() => {
        setLoading(false)
        setInitialLoading(false)
        loadingRef.current = false
      })
  }, [location, category])

  // 위치나 카테고리 변경 시 초기화 후 첫 페이지 로드
  useEffect(() => {
    setShops([])
    setHasNext(false)
    setInitialLoading(true)
    loadingRef.current = false
    fetchPage(0, true)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location, category])

  // IntersectionObserver로 마지막 요소 감지 → 다음 페이지 로드
  const lastCardRef = useCallback(node => {
    if (observerRef.current) observerRef.current.disconnect()

    observerRef.current = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting && hasNext && !loadingRef.current) {
        fetchPage(page + 1, false)
      }
    })

    if (node) observerRef.current.observe(node)
  }, [hasNext, page, fetchPage])

  return (
    <>
      <div className="header">
        <h1>배달이요</h1>
        <Link to="/orders" className="header-link">내 주문</Link>
      </div>

      <div className="location-bar">
        현재 위치 기준으로 배달 가능한 가게를 보여드려요
      </div>

      <div className="categories">
        {CATEGORIES.map(c => (
          <button
            key={c}
            className={`category-btn ${category === c ? 'active' : ''}`}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>

      {initialLoading ? (
        <div className="loading">가게를 불러오는 중...</div>
      ) : (
        <div className="shop-list">
          {shops.length === 0 ? (
            <div className="loading">주변에 배달 가능한 가게가 없습니다</div>
          ) : (
            shops.map((s, index) => {
              const isLast = index === shops.length - 1
              return (
                <Link
                  to={`/shops/${s.id}`}
                  key={`${s.id}-${index}`}
                  className="shop-card"
                  ref={isLast ? lastCardRef : null}
                >
                  {s.imageUrl ? (
                    <img className="shop-img" src={s.imageUrl} alt={s.name} />
                  ) : (
                    <div className="shop-img">
                      {CATEGORY_EMOJI[s.category] || '🏪'}
                    </div>
                  )}
                  <div className="shop-info">
                    <h3>{s.name}{!s.open && <span className="closed-badge">준비중</span>}</h3>
                    <div className="shop-meta">
                      <span className="rating">★ {s.rating}</span>
                      {' · '}{s.distance}km
                    </div>
                    <div className="shop-tags">
                      <span>최소주문 {s.minOrderAmount?.toLocaleString()}원</span>
                      <span>·</span>
                      <span>배달비 {s.deliveryFee === 0 ? '무료' : `${s.deliveryFee?.toLocaleString()}원`}</span>
                    </div>
                  </div>
                </Link>
              )
            })
          )}
          {loading && !initialLoading && (
            <div className="loading">더 불러오는 중...</div>
          )}
        </div>
      )}

      <CartFloatingButton />
    </>
  )
}
