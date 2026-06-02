import React, { useState, useEffect } from 'react'

const BASE = 'http://localhost:8080/api/tasks'

export default function App() {
  const [tasks, setTasks] = useState([])
  const [titulo, setTitulo] = useState('')
  const [descricao, setDescricao] = useState('')

  // Busca todas as tarefas ao carregar
  useEffect(() => {
    fetch(BASE)
      .then(r => r.json())
      .then(setTasks)
  }, [])

  // Cria nova tarefa
  function criar(e) {
    e.preventDefault()
    fetch(BASE, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ titulo, descricao })
    })
      .then(r => r.json())
      .then(nova => {
        setTasks(prev => [...prev, nova])
        setTitulo('')
        setDescricao('')
      })
  }

  // Marca como concluída
  function concluir(id) {
    fetch(`${BASE}/${id}/concluir`, { method: 'PATCH' })
      .then(r => r.json())
      .then(atualizada => {
        setTasks(prev => prev.map(t => t.id === id ? atualizada : t))
      })
  }

  // Deleta tarefa
  function deletar(id) {
    fetch(`${BASE}/${id}`, { method: 'DELETE' })
      .then(() => {
        setTasks(prev => prev.filter(t => t.id !== id))
      })
  }

  return (
    <div style={{ maxWidth: 600, margin: '40px auto', fontFamily: 'Arial' }}>
      <h1>📝 Gerenciador de Tarefas</h1>

      {/* Formulário */}
      <form onSubmit={criar} style={{ marginBottom: 24 }}>
        <input
          value={titulo}
          onChange={e => setTitulo(e.target.value)}
          placeholder="Título da tarefa"
          required
          style={{ width: '100%', padding: 8, marginBottom: 8, boxSizing: 'border-box' }}
        />
        <input
          value={descricao}
          onChange={e => setDescricao(e.target.value)}
          placeholder="Descrição (opcional)"
          style={{ width: '100%', padding: 8, marginBottom: 8, boxSizing: 'border-box' }}
        />
        <button type="submit" style={{ padding: '8px 16px', background: '#1F3864', color: '#fff', border: 'none', cursor: 'pointer' }}>
          Adicionar
        </button>
      </form>

      {/* Lista de tarefas */}
      {tasks.length === 0 && <p>Nenhuma tarefa cadastrada.</p>}
      <ul style={{ listStyle: 'none', padding: 0 }}>
        {tasks.map(t => (
          <li key={t.id} style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            padding: 12, marginBottom: 8, border: '1px solid #ddd', borderRadius: 4,
            background: t.completed ? '#f0f0f0' : '#fff'
          }}>
            <span style={{ textDecoration: t.completed ? 'line-through' : 'none', color: t.completed ? '#888' : '#000' }}>
              <strong>{t.titulo}</strong>
              {t.descricao && <span> — {t.descricao}</span>}
            </span>
            <span>
              {!t.completed && (
                <button onClick={() => concluir(t.id)} style={{ marginRight: 8, padding: '4px 10px', background: '#28a745', color: '#fff', border: 'none', cursor: 'pointer' }}>
                  ✓ Concluir
                </button>
              )}
              <button onClick={() => deletar(t.id)} style={{ padding: '4px 10px', background: '#dc3545', color: '#fff', border: 'none', cursor: 'pointer' }}>
                🗑 Excluir
              </button>
            </span>
          </li>
        ))}
      </ul>
    </div>
  )
}
