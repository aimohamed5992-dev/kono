import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from './api'

const get = (url, params) => api.get(url, { params }).then((r) => r.data)

export const useStats = () => useQuery({ queryKey: ['stats'], queryFn: () => get('/admin/stats') })

export const useLiveBoard = () =>
  useQuery({
    queryKey: ['admin-live'],
    queryFn: () => get('/admin/live'),
    refetchInterval: 10_000,
    staleTime: 0,
  })

export const useAdminOrders = (params) =>
  useQuery({ queryKey: ['admin-orders', params], queryFn: () => get('/admin/orders', params) })

export const useAdminOrder = (id) =>
  useQuery({ queryKey: ['admin-order', id], queryFn: () => get(`/admin/orders/${id}`), enabled: !!id })

export const useAdminStores = (params) =>
  useQuery({ queryKey: ['admin-stores', params], queryFn: () => get('/admin/stores', params) })

export const useAdminStore = (id) =>
  useQuery({ queryKey: ['admin-store', id], queryFn: () => get(`/admin/stores/${id}`), enabled: !!id })

export const useStoreMenu = (storeId) =>
  useQuery({
    queryKey: ['admin-menu', storeId],
    queryFn: () => get(`/admin/stores/${storeId}/menu`),
    enabled: !!storeId,
  })

export const useCouriers = () => useQuery({ queryKey: ['couriers'], queryFn: () => get('/admin/couriers') })

export const useCustomers = (params) =>
  useQuery({ queryKey: ['customers', params], queryFn: () => get('/admin/customers', params) })

export const useVerticals = () => useQuery({ queryKey: ['a-verticals'], queryFn: () => get('/admin/verticals') })

export const usePromotions = () => useQuery({ queryKey: ['a-promotions'], queryFn: () => get('/admin/promotions') })

/** Generic mutation that invalidates given query keys on success. */
export function useApiMutation(fn, invalidate = []) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => invalidate.forEach((k) => qc.invalidateQueries({ queryKey: [k] })),
  })
}
