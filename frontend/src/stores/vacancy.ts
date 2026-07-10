import { defineStore } from 'pinia';
import { ref } from 'vue';
import { vacancyApi } from '@/api/vacancy';
import type { Vacancy, SearchFilters, SearchResponse, VacancyUpdateEvent } from '@/types/vacancy';

export const useVacancyStore = defineStore('vacancy', () => {
  const results = ref<Vacancy[]>([]);
  const total = ref(0);
  const page = ref(0);
  const pageSize = ref(20);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const events = ref<VacancyUpdateEvent[]>([]);
  let unsubscribe: (() => void) | null = null;

  async function search(filters: SearchFilters) {
    loading.value = true;
    error.value = null;
    try {
      const resp: SearchResponse = await vacancyApi.search(filters);
      results.value = resp.results;
      total.value = resp.total;
      page.value = resp.page;
      pageSize.value = resp.pageSize;
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'Search failed';
    } finally {
      loading.value = false;
    }
  }

  function subscribe() {
    if (unsubscribe) return;
    unsubscribe = vacancyApi.subscribeUpdates((evt) => {
      events.value.unshift(evt);
      if (events.value.length > 100) events.value.length = 100;
    });
  }

  function unsubscribeUpdates() {
    if (unsubscribe) { unsubscribe(); unsubscribe = null; }
  }

  return { results, total, page, pageSize, loading, error, events, search, subscribe, unsubscribeUpdates };
});
