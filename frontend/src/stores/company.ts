import { defineStore } from "pinia";
import { ref } from "vue";
import { companyApi } from "@/api/company";
import type { Company } from "@/types/company";

export const useCompanyStore = defineStore("company", () => {
  const companies = ref<Company[]>([]);
  const currentCompany = ref<Company | null>(null);
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

  async function fetchById(id: string) {
    loading.value = true;
    error.value = null;
    currentCompany.value = null;
    try {
      currentCompany.value = await companyApi.getById(id);
    } catch (e) {
      error.value = e instanceof Error ? e.message : "Failed to load company";
    } finally {
      loading.value = false;
    }
  }

  async function fetchByName(name: string) {
    loading.value = true;
    error.value = null;
    currentCompany.value = null;
    try {
      currentCompany.value = await companyApi.getByName(name);
    } catch (e) {
      error.value = e instanceof Error ? e.message : "Failed to load company";
    } finally {
      loading.value = false;
    }
  }

  return { companies, currentCompany, loading, error, fetchAll, fetchById, fetchByName };
});
