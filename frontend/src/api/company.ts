import apiClient from './client';
import type { Company } from '@/types/company';

export const companyApi = {
  list(): Promise<Company[]> {
    return apiClient.get('/companies').then(r => r.data);
  },

  getById(id: string): Promise<Company> {
    return apiClient.get(`/companies/${id}`).then(r => r.data);
  },

  create(data: Partial<Company>): Promise<Company> {
    return apiClient.post('/companies', data).then(r => r.data);
  },

  update(id: string, data: Partial<Company>): Promise<Company> {
    return apiClient.put(`/companies/${id}`, data).then(r => r.data);
  },

  delete(id: string): Promise<void> {
    return apiClient.delete(`/companies/${id}`).then(r => r.data);
  },
};
