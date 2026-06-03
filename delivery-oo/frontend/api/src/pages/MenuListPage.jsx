import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import client from '../api/client'
import CartFloatingButton from '../components/CartFloatingButton'

export default function MenuListPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [shop, setShop] = useState(null)
  const [menus, setMenus] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.all([
      client.get(`/shops/${id}`),
      client.get(`/shops/${id}/menus`)
    ])
      .then(([resInfo, resMenus]) => {
        setShop(resInfo.data)
        setMenus(resMenus.data)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [id])

  // 카테고리별 그룹핑
  const grouped = menus.reduce((acc, menu) => {
    const cat = menu.category || '기타'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(menu)
    return acc
  }, {})

  return (
    <>
      <div className="header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h1>{shop?.name || '메뉴'}</h1>
      </div>

      {shop && (
        <>
          {shop.imageUrl && (
            <img className="shop-hero" src={shop.imageUrl} alt={shop.name} />
          )}
          <div className="shop-detail-section">
            {shop.description && (
              <p className="shop-desc">{shop.description}</p>
            )}
            <div className="shop-detail-bar">
              <span>최소주문 {shop.minOrderAmount?.toLocaleString()}원</span>
              <span>·</span>
              <span>배달비 {shop.deliveryFee === 0 ? '무료' : `${shop.deliveryFee?.toLocaleString()}원`}</span>
              <span>·</span>
              <span>★ {shop.rating}</span>
            </div>
          </div>
        </>
      )}

      {loading ? (
        <div className="loading">메뉴를 불러오는 중...</div>
      ) : (
        Object.entries(grouped).map(([category, items]) => (
          <div key={category}>
            <div className="menu-section-title">{category}</div>
            {items.map(menu => (
              <Link to={`/shops/${id}/menus/${menu.id}`} key={menu.id} className="menu-item">
                <div className="menu-item-info">
                  <h4>{menu.name}</h4>
                  <div className="desc">{menu.description}</div>
                  <div className="price">{menu.price?.toLocaleString()}원</div>
                </div>
              </Link>
            ))}
          </div>
        ))
      )}

      <CartFloatingButton />
      <div style={{ height: 80 }} />
    </>
  )
}
