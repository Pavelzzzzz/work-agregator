<template>
  <div>
    <div class="flex items-center gap-3 mb-6">
      <input
        v-model="query"
        type="text"
        placeholder="Search vacancies..."
        class="flex-1 border rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        @keyup.enter="doSearch"
      />
      <select v-model="filters.employmentType" class="border rounded-lg px-3 py-2 text-sm">
        <option value="">All types</option>
        <option value="FULL_TIME">Full-time</option>
        <option value="PART_TIME">Part-time</option>
        <option value="CONTRACT">Contract</option>
        <option value="INTERNSHIP">Internship</option>
      </select>
      <select v-model="filters.source" class="border rounded-lg px-3 py-2 text-sm">
        <option value="">All sources</option>
        <option value="hh.ru">hh.ru</option>
        <option value="linkedin">LinkedIn</option>
        <option value="djinni">Djinni</option>
      </select>
      <label class="flex items-center gap-2 text-sm cursor-pointer">
        <input type="checkbox" v-model="filters.remoteOnly" class="rounded" />
        Remote only
      </label>
      <button @click="doSearch"
        class="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-blue-700">
        Search
      </button>
    </div>
    <div v-if="store.loading"><Spinner /></div>
    <div v-else-if="store.error" class="text-red-600 text-sm p-4 bg-red-50 rounded-lg">
      {{ store.error }}
    </div>
    <div v-else-if="!store.results.length" class="text-gray-400 text-center py-12">
      No vacancies found. Try adjusting your search.
    </div>
    <div v-else class="space-y-3">
      <div class="text-sm text-gray-500 mb-2">{{ store.total }} vacancies found</div>
      <VacancyCard
        v-for="v in store.results"
        :key="v.id"
        :title="v.translations?.[0]?.title || 'Untitled'"
        :companyName="v.translations?.[0]?.companyName || ''"
        :employmentType="v.employmentType"
        :remote="v.remote"
        :skills="v.skills"
        :location="v.location"
        :salaryMin="v.salaryMin"
        :salaryMax="v.salaryMax"
        :salaryCurrency="v.salaryCurrency"
        :postedAt="formatDate(v.postedAt)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useVacancyStore } from '@/stores/vacancy';
import VacancyCard from '@/components/vacancies/VacancyCard.vue';
import Spinner from '@/components/common/Spinner.vue';

const store = useVacancyStore();
const query = ref('');
const filters = reactive({
  employmentType: '',
  source: '',
  remoteOnly: false,
});

function doSearch() {
  store.search({ query: query.value || undefined, ...filters, remoteOnly: filters.remoteOnly || undefined });
}

function formatDate(d: string) {
  try { return new Date(d).toLocaleDateString(); } catch { return d; }
}
</script>
