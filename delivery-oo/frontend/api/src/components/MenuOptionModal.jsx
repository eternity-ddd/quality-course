import { useState } from 'react'
import client from '../api/client'
import { getSessionId } from '../pages/ShopListPage'

export default function MenuOptionModal({ menu, shopId, onClose }) {
  const [quantity, setQuantity] = useState(1)
  const [selectedOptions, setSelectedOptions] = useState({})
  const [showConflict, setShowConflict] = useState(false)

  const handleOptionChange = (group, option) => {
    setSelectedOptions(prev => {
      const next = { ...prev }
      if (group.maxSelect === 1) {
        next[group.id] = [option]
      } else {
        const current = next[group.id] || []
        const exists = current.find(o => o.id === option.id)
        if (exists) {
          next[group.id] = current.filter(o => o.id !== option.id)
        } else if (current.length < group.maxSelect) {
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
    let optionPrice = 0
    Object.values(selectedOptions).forEach(opts => {
      opts.forEach(o => { optionPrice += o.price || 0 })
    })
    return (menu.price + optionPrice) * quantity
  }

  const buildRequest = () => {
    const groupNameById = {}
    ;(menu.optionGroups || []).forEach(g => { groupNameById[g.id] = g.name })

    const flatOptions = []
    Object.entries(selectedOptions).forEach(([groupId, opts]) => {
      const numericGroupId = Number(groupId)
      opts.forEach(o => {
        flatOptions.push({
          optionGroupId: numericGroupId,
          optionGroupName: groupNameById[numericGroupId],
          name: o.name,
          price: o.price,
        })
      })
    })
    return {
      sessionId: getSessionId(),
      menuId: menu.id,
      menuName: menu.name,
      quantity,
      selectedOptions: flatOptions,
    }
  }

  const addToCart = () => {
    client.post('/cart/items', buildRequest())
      .then(() => {
        onClose()
        window.dispatchEvent(new Event('cart-updated'))
      })
      .catch(err => {
        console.error(err)
        alert('장바구니 추가에 실패했습니다')
      })
  }

  const handleAddToCart = () => {
    // 현재 장바구니 확인 → 다른 가게면 확인 다이얼로그
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

  const handleConfirmReplace = () => {
    setShowConflict(false)
    addToCart()
  }

  const requiredGroups = (menu.optionGroups || []).filter(g => g.required)
  const allRequiredSelected = requiredGroups.every(g =>
    selectedOptions[g.id] && selectedOptions[g.id].length > 0
  )

  return (
    <>
      <div className="modal-overlay" onClick={onClose}>
        <div className="modal-content" onClick={e => e.stopPropagation()}>
          <button className="modal-close" onClick={onClose}>×</button>

          <div className="modal-header">
            <h3>{menu.name}</h3>
            {menu.description && <div className="modal-desc">{menu.description}</div>}
            <div className="modal-price">{menu.price?.toLocaleString()}원</div>
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
                      type={group.maxSelect === 1 ? 'radio' : 'checkbox'}
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
            >
              -
            </button>
            <span className="qty-num">{quantity}</span>
            <button
              className="qty-btn"
              onClick={() => setQuantity(q => q + 1)}
            >
              +
            </button>
          </div>

          <button
            className="add-to-cart-btn"
            onClick={handleAddToCart}
            disabled={!allRequiredSelected}
          >
            {allRequiredSelected
              ? `${calcTotalPrice().toLocaleString()}원 담기`
              : '필수 옵션을 선택해주세요'
            }
          </button>
        </div>
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
              <button className="dialog-cancel" onClick={() => setShowConflict(false)}>
                취소
              </button>
              <button className="dialog-confirm" onClick={handleConfirmReplace}>
                담기
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
