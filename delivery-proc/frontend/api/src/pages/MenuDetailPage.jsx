import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import client from '../api/client'
import { getSessionId } from './ShopListPage'

export default function MenuDetailPage() {
  const { id: shopId, menuId } = useParams()
  const navigate = useNavigate()
  const [menu, setMenu] = useState(null)
  const [shop, setShop] = useState(null)
  const [loading, setLoading] = useState(true)
  const [quantity, setQuantity] = useState(1)
  const [selectedOptions, setSelectedOptions] = useState({})
  const [showConflict, setShowConflict] = useState(false)

  useEffect(() => {
    Promise.all([
      client.get(`/shops/${shopId}/menus/${menuId}`),
      client.get(`/shops/${shopId}`)
    ])
      .then(([menuRes, shopRes]) => {
        setMenu(menuRes.data)
        setShop(shopRes.data)
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [shopId, menuId])

  const handleOptionChange = (group, option) => {
    setSelectedOptions(prev => {
      const next = { ...prev }
      if (group.required) {
        next[group.id] = [option]
      } else {
        const current = next[group.id] || []
        const exists = current.find(o => o.id === option.id)
        if (exists) {
          next[group.id] = current.filter(o => o.id !== option.id)
        } else {
          next[group.id] = [...current, option]
        }
      }
      return next
    })
  }

  const isOptionSelected = (groupId, optionId) => {
    return (selectedOptions[groupId] || []).some(o => o.id === optionId)
  }

  const calcTotalPrice = () => {
    if (!menu) return 0
    let optionPrice = 0
    Object.values(selectedOptions).forEach(opts => {
      opts.forEach(o => { optionPrice += o.price || 0 })
    })
    return (menu.price + optionPrice) * quantity
  }

  const buildRequest = () => {
    const groupNameById = {}
    ;(menu?.optionGroups || []).forEach(g => { groupNameById[g.id] = g.name })

    const flatOptions = []
    Object.entries(selectedOptions).forEach(([groupId, opts]) => {
      const numericGroupId = Number(groupId)
      opts.forEach(o => {
        flatOptions.push({
          optionGroupId: numericGroupId,
          optionGroupName: groupNameById[numericGroupId],
          optionId: o.id,
          name: o.name,
          price: o.price,
        })
      })
    })
    return {
      sessionId: getSessionId(),
      menuId: Number(menuId),
      menuName: menu?.name,
      quantity,
      selectedOptions: flatOptions,
    }
  }

  const addToCart = () => {
    client.post('/cart/items', buildRequest())
      .then(() => {
        window.dispatchEvent(new Event('cart-updated'))
        navigate(-1)
      })
      .catch(() => alert('장바구니 추가에 실패했습니다'))
  }

  const handleAddToCart = () => {
    client.get('/cart', { params: { sessionId: getSessionId() } })
      .then(res => {
        const cart = res.data
        if (cart.shop && String(cart.shop.id) !== String(shopId) && cart.items.length > 0) {
          setShowConflict(true)
        } else {
          addToCart()
        }
      })
      .catch(() => addToCart())
  }

  if (loading) return <div className="loading">메뉴를 불러오는 중...</div>
  if (!menu) return <div className="loading">메뉴를 찾을 수 없습니다</div>

  const shopOpen = shop?.open !== false
  const requiredGroups = (menu.optionGroups || []).filter(g => g.required)
  const allRequiredSelected = requiredGroups.every(g =>
    selectedOptions[g.id] && selectedOptions[g.id].length > 0
  )
  const canAddToCart = shopOpen && allRequiredSelected

  return (
    <>
      <div className="header">
        <button className="back-btn" onClick={() => navigate(-1)}>←</button>
        <h1>메뉴 상세</h1>
      </div>

      {menu.imageUrl && (
        <img className="menu-detail-hero" src={menu.imageUrl} alt={menu.name} />
      )}

      <div className="menu-detail-info">
        <h2 className="menu-detail-name">
          {menu.name}
        </h2>
        {menu.description && (
          <p className="menu-detail-desc">{menu.description}</p>
        )}
        <div className="menu-detail-price">{menu.price?.toLocaleString()}원</div>
      </div>

      {(menu.optionGroups || []).map(group => (
        <div key={group.id} className="option-group">
          <div className="option-group-header">
            <h4>{group.name}</h4>
            {group.required && <span className="required-badge">필수</span>}
          </div>
          {group.options.map(option => (
            <div key={option.id} className="option-item">
              <label>
                <input
                  type={group.required ? 'radio' : 'checkbox'}
                  name={`group-${group.id}`}
                  checked={isOptionSelected(group.id, option.id)}
                  onChange={() => handleOptionChange(group, option)}
                />
                {option.name}
              </label>
              <span className="option-price">
                {option.price > 0 && `+${option.price.toLocaleString()}원`}
                {option.price < 0 && `${option.price.toLocaleString()}원`}
              </span>
            </div>
          ))}
        </div>
      ))}

      <div className="quantity-control">
        <button
          className="qty-btn"
          disabled={quantity <= 1}
          onClick={() => setQuantity(q => Math.max(1, q - 1))}
        >-</button>
        <span className="qty-num">{quantity}</span>
        <button className="qty-btn" onClick={() => setQuantity(q => q + 1)}>+</button>
      </div>

      <div style={{ padding: '0 16px 100px' }}>
        <button
          className="add-to-cart-btn"
          onClick={handleAddToCart}
          disabled={!canAddToCart}
        >
          {!shopOpen
            ? '현재 준비중입니다'
            : allRequiredSelected
              ? `${calcTotalPrice().toLocaleString()}원 담기`
              : '필수 옵션을 선택해주세요'
          }
        </button>
      </div>

      {showConflict && (
        <div className="dialog-overlay" onClick={() => setShowConflict(false)}>
          <div className="dialog-box" onClick={e => e.stopPropagation()}>
            <p className="dialog-message">
              장바구니에는 같은 가게의 메뉴만 담을 수 있습니다.
            </p>
            <p className="dialog-sub">
              선택하신 메뉴를 장바구니에 담을 경우 이전에 담은 메뉴가 삭제됩니다.
            </p>
            <div className="dialog-buttons">
              <button className="dialog-cancel" onClick={() => setShowConflict(false)}>취소</button>
              <button className="dialog-confirm" onClick={() => { setShowConflict(false); addToCart(); }}>담기</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
