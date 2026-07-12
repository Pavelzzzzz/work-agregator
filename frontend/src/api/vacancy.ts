import apiClient from "./client";
import type { SearchFilters, SearchResponse, VacancyUpdateEvent, Vacancy } from "@/types/vacancy";

export const vacancyApi = {
  getById(id: string): Promise<Vacancy> {
    return apiClient.get(`/vacancies/${id}`).then((r) => r.data);
  },

  search(filters: SearchFilters): Promise<SearchResponse> {
    const params: Record<string, string | number | boolean> = {};
    if (filters.query) params.q = filters.query;
    if (filters.language) params.language = filters.language;
    if (filters.source) params.source = filters.source;
    if (filters.employmentType) params.employmentType = filters.employmentType;
    if (filters.minSalary !== undefined && filters.minSalary !== null)
      params.minSalary = filters.minSalary;
    if (filters.maxSalary !== undefined && filters.maxSalary !== null)
      params.maxSalary = filters.maxSalary;
    if (filters.skills?.length) params.skills = filters.skills.join(",");
    if (filters.companyName) params.companyName = filters.companyName;
    if (filters.location) params.location = filters.location;
    if (filters.remoteOnly) params.remoteOnly = true;
    if (filters.page) params.page = filters.page;
    if (filters.pageSize) params.pageSize = filters.pageSize;
    return apiClient.get("/vacancies/search", { params }).then((r) => r.data);
  },

  subscribeUpdates(onEvent: (event: VacancyUpdateEvent) => void): () => void {
    const es = new EventSource("/api/vacancies/updates");
    es.onmessage = (msg) => {
      try {
        onEvent(JSON.parse(msg.data));
      } catch {
        /* ignore parse errors */
      }
    };
    return () => es.close();
  },
};
