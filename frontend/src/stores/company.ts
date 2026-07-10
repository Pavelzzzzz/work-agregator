import { defineStore } from "pinia";
import { ref } from "vue";
import { companyApi } from "@/api/company";
import type { Company } from "@/types/company";

export const useCompanyStore = defineStore("company", () => {
  const companies = ref<Company[]>([]);
  const loading = ref(false);
  const error = ref<string | null>(null);

  async function fetchAll() {
    loading.value = true;
    error.value = null;
    try {
      companies.value = await companyApi.list();
    } catch (e) {
      error.value = e instanceof Error ? e.message : "Failed to load companies";
    } finally {
      loading.value = false;
    }
  }

  return { companies, loading, error, fetchAll };
});
