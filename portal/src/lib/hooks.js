import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

const get = (url, params) => api.get(url, { params }).then((r) => r.data)

export const useTickets = (state) =>
  useQuery({
    queryKey: ['tickets', state ?? 'all'],
    queryFn: () => get('/merchant/tickets', state ? { state } : undefined),
    refetchInterval: 8000,
  })

export const useTicketDetail = (orderId) =>
  useQuery({
    queryKey: ['ticket', orderId],
    queryFn: () => get(`/merchant/tickets/${orderId}`),
    enabled: !!orderId,
  })

export const useMenu = () => useQuery({ queryKey: ['menu'], queryFn: () => get('/merchant/menu') })

export const useReports = () => useQuery({ queryKey: ['reports'], queryFn: () => get('/merchant/reports') })

export const useStore = () => useQuery({ queryKey: ['store'], queryFn: () => get('/merchant/store') })

export const useThreads = () =>
  useQuery({ queryKey: ['threads'], queryFn: () => get('/chat/threads'), refetchInterval: 10000 })

export function useApiMutation(fn, invalidate = []) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => invalidate.forEach((k) => qc.invalidateQueries({ queryKey: [k] })),
  })
}
