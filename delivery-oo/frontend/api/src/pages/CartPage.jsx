import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import { getSessionId } from './ShopListPage'

export default function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(true)

  const fetchCart = () => {
    client.get('/cart', { params: { sessionId: getSessionId() } })
      .then(res => setCart(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchCart() }, [])

  const updateQuantity = (itemId, quantity) => {
    const sid = getSessionId()
    if (quantity < 1) {
      client.delete(`/cart/items/${itemId}`, { params: { sessionId: sid } })
        .then(res => setCart(res.data))
    } else {
      client.patch(`/cart/items/${itemId}?sessionId=${sid}`, { quantity })
        .then(res => setCart(res.data))
    }
  }

  const removeItem = (itemId) => {
    client.delete(`/cart/items/${itemId}`, { params: { sessionId: getSessionId() } })
      .then(res => setCart(res.data))
  }

  const formatOptions = (options) => {
    if (!options || options.length === 0) return ''
    return options.map(o => o.name).join(', ')
  }

  if (loading) return <div className="loading">불러오는 중...</div>

  const items = cart?.items || []
  const deliveryFee = cart?.shop?.deliveryFee || 0
  const minOrder = cart?.shop?.minOrderAmount || 0
  const shopOpen = cart?.shop?.open !== false
  const totalPrice = cart?.totalPrice || 0
  const finalTotal = totalPrice + deliveryFee

  return (
    <>
      <div className="header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h1>장바구니</h1>
      </div>

      {items.length === 0 ? (
        <div className="cart-empty">
          <p>장바구니가 비어있습니다</p>
          <button className="link-btn" onClick={() => navigate('/')}>
            가게 보러가기
          </button>
        </div>
      ) : (
        <>
          <div className="cart-shop-name">
            {cart.shop?.name}에서 주문
          </div>

          {items.map(item => (
            <div key={item.id} className="cart-item">
              <h4>{item.menuName}</h4>
              {formatOptions(item.selectedOptions) && (
                <div className="cart-item-options">
                  {formatOptions(item.selectedOptions)}
                </div>
              )}
              <div className="cart-item-bottom">
                <div className="cart-item-qty">
                  <button onClick={() => updateQuantity(item.id, item.quantity - 1)}>-</button>
                  <span>{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.id, item.quantity + 1)}>+</button>
                </div>
                <span className="item-price">
                  {(item.unitPrice * item.quantity).toLocaleString()}원
                </span>
              </div>
            </div>
          ))}

          <div className="cart-summary">
            <div className="cart-summary-row">
              <span>주문금액</span>
              <span>{totalPrice.toLocaleString()}원</span>
            </div>
            <div className="cart-summary-row">
              <span>배달비</span>
              <span>{deliveryFee === 0 ? '무료' : `${deliveryFee.toLocaleString()}원`}</span>
            </div>
            <div className="cart-summary-row total">
              <span>총 결제금액</span>
              <span>{finalTotal.toLocaleString()}원</span>
            </div>
          </div>

          <button
            className="order-btn"
            disabled={!shopOpen || totalPrice < minOrder}
            onClick={() => {
              client.post(`/cart/order?sessionId=${getSessionId()}`)
                .then(() => {
                  alert('주문이 완료되었습니다!')
                  window.dispatchEvent(new Event('cart-updated'))
                  navigate('/')
                })
                .catch(() => alert('주문에 실패했습니다.'))
            }}
          >
            {!shopOpen
              ? '현재 준비중입니다'
              : totalPrice < minOrder
                ? `최소주문금액 ${minOrder.toLocaleString()}원 이상 주문해주세요`
                : `${finalTotal.toLocaleString()}원 주문하기`
            }
          </button>

          <div style={{ height: 20 }} />
        </>
      )}
    </>
  )
}
