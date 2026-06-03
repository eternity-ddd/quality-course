import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'
import { getSessionId } from '../pages/ShopListPage'

export default function CartFloatingButton() {
  const navigate = useNavigate()
  const [cart, setCart] = useState(null)

  const fetchCart = () => {
    client.get('/cart', { params: { sessionId: getSessionId() } })
      .then(res => setCart(res.data))
      .catch(() => {})
  }

  useEffect(() => {
    fetchCart()
    const handler = () => fetchCart()
    window.addEventListener('cart-updated', handler)
    return () => window.removeEventListener('cart-updated', handler)
  }, [])

  const itemCount = cart?.items?.length || 0
  if (itemCount === 0) return null

  return (
    <button className="floating-cart" onClick={() => navigate('/cart')}>
      <span className="cart-count">{itemCount}</span>
      <span className="floating-cart-center">장바구니 보기</span>
      <span>{cart.totalPrice?.toLocaleString()}원</span>
    </button>
  )
}
