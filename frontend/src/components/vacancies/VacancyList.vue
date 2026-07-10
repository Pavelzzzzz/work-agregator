<template>
  <div>
    <div class="flex flex-wrap items-center gap-3 mb-6">
      <input
        v-model="query"
        type="text"
        placeholder="Search vacancies..."
        class="flex-1 min-w-[200px] border rounded-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        @keyup.enter="doSearch"
      >
      <input
        v-model="filters.skillsText"
        type="text"
        placeholder="Skills (comma separated)"
        class="w-48 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
      <input
        v-model="filters.minSalary"
        type="number"
        placeholder="Salary min"
        class="w-28 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
      <input
        v-model="filters.maxSalary"
        type="number"
        placeholder="Salary max"
        class="w-28 border rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
      <select v-model="filters.employmentType" class="border rounded-lg px-3 py-2 text-sm">
        <option value="">All types</option>
        <option value="FULL_TIME">Full-time</option>
        <option value="PART_TIME">Part-time</option>
        <option value="CONTRACT">Contract</option>
        <option value="INTERNSHIP">Internship</option>
      </select>
      <label class="flex items-center gap-2 text-sm cursor-pointer whitespace-nowrap">
        <input v-model="filters.remoteOnly" type="checkbox" class="rounded">
        Remote only
      </label>
      <button
        class="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
        @click="doSearch"
      >
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
        :title="v.title || 'Untitled'"
        :company-name="v.companyName || ''"
        :employment-type="v.employmentType"
        :remote="!v.location"
        :skills="v.skills"
        :location="v.location"
        :salary-min="v.salaryMin"
        :salary-max="v.salaryMax"
        :salary-currency="v.salaryCurrency"
        :posted-at="formatDate(v.postedAt)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { useVacancyStore } from "@/stores/vacancy";
import VacancyCard from "@/components/vacancies/VacancyCard.vue";
import Spinner from "@/components/common/Spinner.vue";

const store = useVacancyStore();
const query = ref("");

const filters = reactive({
  employmentType: "",
  remoteOnly: false,
  minSalary: undefined as number | undefined,
  maxSalary: undefined as number | undefined,
  skillsText: "",
});

function doSearch() {
  const skills = filters.skillsText
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

  store.search({
    query: query.value || undefined,
    employmentType: filters.employmentType || undefined,
    remoteOnly: filters.remoteOnly || undefined,
    minSalary: filters.minSalary || undefined,
    maxSalary: filters.maxSalary || undefined,
    skills: skills.length ? skills : undefined,
  });
}

function formatDate(d: string) {
  try {
    return new Date(d).toLocaleDateString();
  } catch {
    return d;
  }
}
</script>
