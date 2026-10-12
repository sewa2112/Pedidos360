import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [orders, setOrders] = useState([]);
  const [status, setStatus] = useState('Conectando...');
  const [stats, setStats] = useState({ total: 0, success: 0, failed: 0 });

  // Cargar estado inicial
  useEffect(() => {
    checkBackendStatus();
    const interval = setInterval(checkBackendStatus, 5000);
    return () => clearInterval(interval);
  }, []);

  const checkBackendStatus = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/orders/status');
      if (response.ok) {
        setStatus('✓ Conectado a Backend');
      }
    } catch {
      setStatus('✗ Backend desconectado');
    }
  };

  const sendOrder = async (customerName) => {
    const orderId = `ORD-${Date.now()}`;

    try {
      const response = await fetch('http://localhost:8080/api/orders/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ orderId, customerName }),
      });
      if (response.ok) {
        const data = await response.json();

        // Agregar a la lista de órdenes
        const newOrder = {
          id: orderId,
          customer: customerName,
          message: data.message,
          timestamp: new Date().toLocaleTimeString(),
          status: 'ENVIADA', // Pendiente confirmación del servidor
        };

        setOrders(prevOrders => [newOrder, ...prevOrders]);
        setStats(prev => ({ ...prev, total: prev.total + 1 }));

        // Simular resultado después de 2 segundos (en realidad verías en los logs)
        setTimeout(() => {
          updateOrderStatus(orderId);
        }, 2000);
      }
    } catch (error) {
      console.error('Error:', error);
      alert('Error al enviar la orden');
    }
  };

  const updateOrderStatus = (orderId) => {
    // En una app real, esto vendría de un WebSocket
    // Por ahora, simulamos que hay 60% de éxito
    const isSuccess = Math.random() > 0.4;

    setOrders(prevOrders =>
      prevOrders.map(order =>
        order.id === orderId
          ? {
              ...order,
              status: isSuccess ? 'PROCESADA' : 'FALLIDA (DLQ)',
            }
          : order
      )
    );
    if (isSuccess) {
      setStats(prev => ({ ...prev, success: prev.success + 1 }));
    } else {
      setStats(prev => ({ ...prev, failed: prev.failed + 1 }));
    }
  };

  return (
    <div className="App">
      <header className="header">
        <h1>Sistema de Órdenes con RabbitMQ</h1>
        <p className="status-badge">{status}</p>
      </header>
      <section className="control-panel">
        <h2>Enviar Órdenes</h2>
        <div className="button-group">
          <button onClick={() => sendOrder('Juan Pérez')} className="btn btn-primary">
            Orden Cliente 1
          </button>
          <button onClick={() => sendOrder('María García')} className="btn btn-primary">
            Orden Cliente 2
          </button>
          <button onClick={() => sendOrder('Carlos López')} className="btn btn-primary">
            Orden Cliente 3
          </button>
        </div>
      </section>
      <section className="stats-panel">
        <div className="stat-card">
          <div className="stat-number">{stats.total}</div>
          <div className="stat-label">Total Órdenes</div>
        </div>
        <div className="stat-card success">
          <div className="stat-number">{stats.success}</div>
          <div className="stat-label">Procesadas</div>
        </div>
        <div className="stat-card error">
          <div className="stat-number">{stats.failed}</div>
          <div className="stat-label">En DLQ</div>
        </div>
      </section>
      <section className="orders-list">
        <h2>Historial de Órdenes</h2>
        {orders.length === 0 ? (
          <p className="empty-message">No hay órdenes aún. ¡Envía una!</p>
        ) : (
          <div className="orders-table">
            {orders.map(order => {
              const statusClass = order.status.includes('PROCESADA')
                ? 'status-PROCESADA'
                : order.status.includes('FALLIDA')
                  ? 'status-FALLIDA'
                  : 'status-ENVIADA';
              return (
                <div key={order.id} className={`order-item ${statusClass}`}>
                  <div className="order-header">
                    <strong>{order.id}</strong>
                    <span className={`status-badge ${statusClass}`}>
                      {order.status}
                    </span>
                  </div>
                  <div className="order-detail">
                    <span>Cliente: {order.customer}</span>
                    <span className="timestamp">{order.timestamp}</span>
                  </div>
                  <div className="order-message">{order.message}</div>
                </div>
              );
            })}
          </div>
        )}
      </section>
      <footer className="footer">
        <p>Tip: Abre la consola de RabbitMQ (localhost:15672) para ver las colas en tiempo real</p>
      </footer>
    </div>
  );
}

export default App;

