import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'

const PAGE_SIZE = 5

function formatDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export default function OrderHistoryPage() {
  const navigate = useNavigate()
  const [orders, setOrders] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setLoading(true)
    client.get('/orders', { params: { page, size: PAGE_SIZE } })
      .then(res => {
        const data = res.data
        setOrders(Array.isArray(data?.content) ? data.content : [])
        setTotalPages(data?.totalPages || 0)
        setTotalElements(data?.totalElements || 0)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [page])

  return (
    <>
      <div className="header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h1>내 주문 내역</h1>
      </div>

      {loading ? (
        <div className="loading">불러오는 중...</div>
      ) : orders.length === 0 ? (
        <div className="cart-empty">
          <p>주문 내역이 없습니다</p>
          <button className="link-btn" onClick={() => navigate('/')}>가게 보러가기</button>
        </div>
      ) : (
        <>
          <div className="order-count">총 {totalElements}건</div>

          {orders.map(order => (
            <div key={order.id} className="order-card">
              <div className="order-card-head">
                <span className="order-shop">{order.shopName || `가게 #${order.shopId}`}</span>
                <span className="order-time">{formatDate(order.orderedTime)}</span>
              </div>
              {order.items.map((it, idx) => (
                <div key={idx} className="order-item">
                  <div className="order-item-name">{it.menuName} × {it.quantity}</div>
                  {it.options.length > 0 && (
                    <div className="order-item-options">
                      {it.options.map(o => o.name).join(', ')}
                    </div>
                  )}
                  <div className="order-item-subtotal">{it.subtotal.toLocaleString()}원</div>
                </div>
              ))}
              <div className="order-card-foot">
                <span>총 결제금액</span>
                <span className="order-total">{order.totalPrice.toLocaleString()}원</span>
              </div>
            </div>
          ))}

          <div className="pagination">
            <button
              className="page-btn"
              disabled={page === 0}
              onClick={() => setPage(p => Math.max(0, p - 1))}
            >이전</button>
            <span className="page-indicator">{page + 1} / {Math.max(totalPages, 1)}</span>
            <button
              className="page-btn"
              disabled={page >= totalPages - 1}
              onClick={() => setPage(p => p + 1)}
            >다음</button>
          </div>
        </>
      )}
    </>
  )
}
