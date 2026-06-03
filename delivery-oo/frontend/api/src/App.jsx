import { Routes, Route } from 'react-router-dom'
import ShopListPage from './pages/ShopListPage'
import MenuListPage from './pages/MenuListPage'
import MenuDetailPage from './pages/MenuDetailPage'
import CartPage from './pages/CartPage'
import OrderHistoryPage from './pages/OrderHistoryPage'

function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<ShopListPage />} />
        <Route path="/shops/:id" element={<MenuListPage />} />
        <Route path="/shops/:id/menus/:menuId" element={<MenuDetailPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/orders" element={<OrderHistoryPage />} />
      </Routes>
    </div>
  )
}

export default App
