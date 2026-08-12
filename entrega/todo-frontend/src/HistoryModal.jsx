import React, { useEffect, useState } from 'react'

const BASE = 'http://localhost:8080/api/tasks'

const ACTION_LABELS = {
  CREATED: 'Criada',
  UPDATED: 'Atualizada',
  COMPLETED: 'Concluída',
  DELETED: 'Excluída',
}

function formatarData(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('pt-BR')
}

function formatarDuracao(segundos) {
  if (segundos == null) return '—'
  const horas = Math.floor(segundos / 3600)
  const minutos = Math.floor((segundos % 3600) / 60)
  if (horas === 0) return `${minutos} min`
  return `${horas}h ${minutos}min`
}

/**
 * Painel de histórico/estatísticas de uma task.
 * Os dados vêm do history-service (microsserviço), acessados através dos
 * endpoints /api/tasks/{id}/historico e /api/tasks/{id}/estatisticas do
 * todo-api — o front-end continua falando só com um host.
 */
export default function HistoryModal({ taskId, titulo, onClose }) {
  const [eventos, setEventos] = useState([])
  const [stats, setStats] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState(null)

  useEffect(() => {
    setCarregando(true)
    setErro(null)

    Promise.all([
      fetch(`${BASE}/${taskId}/historico`).then(r => (r.ok ? r.json() : Promise.reject(r.status))),
      fetch(`${BASE}/${taskId}/estatisticas`).then(r => (r.ok ? r.json() : Promise.reject(r.status))),
    ])
      .then(([historico, estatisticas]) => {
        setEventos(historico)
        setStats(estatisticas)
      })
      .catch(() => setErro('Não foi possível carregar o histórico (history-service indisponível?).'))
      .finally(() => setCarregando(false))
  }, [taskId])

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)',
        display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
      }}
    >
      <div
        onClick={e => e.stopPropagation()}
        style={{
          background: '#fff', borderRadius: 8, padding: 24,
          width: '90%', maxWidth: 480, maxHeight: '80vh', overflowY: 'auto'
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h2 style={{ margin: 0, fontSize: 18 }}>Histórico — {titulo}</h2>
          <button onClick={onClose} style={{ border: 'none', background: 'none', fontSize: 18, cursor: 'pointer' }}>✕</button>
        </div>

        {carregando && <p>Carregando…</p>}
        {erro && <p style={{ color: '#dc3545' }}>{erro}</p>}

        {!carregando && !erro && stats && (
          <div style={{
            display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8,
            background: '#f7f7f7', borderRadius: 6, padding: 12, marginBottom: 16, fontSize: 13
          }}>
            <div><strong>Eventos:</strong> {stats.totalEventos}</div>
            <div><strong>Conclusões:</strong> {stats.quantidadeConclusoes}</div>
            <div><strong>Reaberturas:</strong> {stats.reaberturas}</div>
            <div><strong>Tempo até 1ª conclusão:</strong> {formatarDuracao(stats.tempoAteConclusaoSegundos)}</div>
          </div>
        )}

        {!carregando && !erro && (
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {eventos.map(ev => (
              <li key={ev.id} style={{
                borderLeft: '3px solid #1F3864', paddingLeft: 10, marginBottom: 10
              }}>
                <div style={{ fontWeight: 'bold' }}>{ACTION_LABELS[ev.action] || ev.action}</div>
                <div style={{ fontSize: 12, color: '#666' }}>{formatarData(ev.changedAt)}</div>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  )
}
