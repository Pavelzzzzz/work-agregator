<template>
  <div>
    <router-link to="/vacancies" class="text-sm text-blue-600 hover:underline mb-4 inline-block">
      &larr; Back to vacancies
    </router-link>
    <div v-if="store.loading"><Spinner /></div>
    <div v-else-if="store.error" class="text-red-600 text-sm p-4 bg-red-50 rounded-lg">
      {{ store.error }}
    </div>
    <div v-else-if="v" class="bg-white border rounded-lg p-6">
      <div class="flex justify-between items-start gap-4">
        <div class="flex-1 min-w-0">
          <h1 class="text-2xl font-bold text-gray-900">{{ v.title }}</h1>
          <router-link :to="companyRoute" class="text-lg text-blue-600 hover:underline mt-1 block">
            {{ v.companyName }}
          </router-link>
        </div>
        <div v-if="salary" class="text-lg font-semibold text-green-700 whitespace-nowrap">
          {{ salary }}
        </div>
      </div>

      <div class="flex flex-wrap gap-2 mt-4">
        <StatusBadge v-if="v.employmentType" variant="blue">{{ v.employmentType }}</StatusBadge>
        <StatusBadge v-if="!v.location" variant="green">Remote</StatusBadge>
        <StatusBadge v-for="s in v.skills" :key="s" variant="gray">{{ s }}</StatusBadge>
      </div>

      <div class="mt-4 text-sm text-gray-500 space-y-1">
        <p v-if="v.location"><strong>Location:</strong> {{ v.location }}</p>
        <p v-if="v.experienceRequired"><strong>Experience:</strong> {{ v.experienceRequired }}</p>
        <p><strong>Posted:</strong> {{ formatDate(v.postedAt) }}</p>
        <p><strong>Source:</strong> {{ v.sourceName }}</p>
      </div>

      <div v-if="v.description" class="mt-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Description</h2>
        <div class="prose prose-sm max-w-none text-gray-700" v-html="v.description" />
      </div>

      <div v-if="v.requirements" class="mt-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Requirements</h2>
        <div class="prose prose-sm max-w-none text-gray-700" v-html="v.requirements" />
      </div>

      <div v-if="v.responsibilities" class="mt-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">Responsibilities</h2>
        <div class="prose prose-sm max-w-none text-gray-700" v-html="v.responsibilities" />
      </div>

      <a
        v-if="v.url"
        :href="v.url"
        target="_blank"
        rel="noopener noreferrer"
        class="inline-block mt-6 bg-blue-600 text-white px-6 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
      >
        Open on {{ v.sourceName }} &rarr;
      </a>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from "vue";
import { useRoute } from "vue-router";
import { useVacancyStore } from "@/stores/vacancy";
import Spinner from "@/components/common/Spinner.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";

const route = useRoute();
const store = useVacancyStore();

const v = computed(() => store.currentVacancy);

const companyRoute = computed(() => {
  const name = v.value?.companyName;
  if (!name) return "";
  return `/companies/${encodeURIComponent(name)}`;
});

const salary = computed(() => {
  if (!v.value) return null;
  const { salaryMin, salaryMax, salaryCurrency } = v.value;
  if (salaryMin === null && salaryMax === null) return null;
  const fmt = (val: number) => val.toLocaleString();
  const cur = salaryCurrency || "";
  if (salaryMin !== null && salaryMax !== null) return `${fmt(salaryMin)}–${fmt(salaryMax)} ${cur}`;
  if (salaryMin !== null) return `from ${fmt(salaryMin)} ${cur}`;
  return `up to ${fmt(salaryMax!)} ${cur}`;
});

function formatDate(d: string) {
  try {
    return new Date(d).toLocaleDateString();
  } catch {
    return d;
  }
}

function loadVacancy(id: string) {
  if (id) store.fetchById(id);
}

onMounted(() => loadVacancy(route.params.id as string));

watch(
  () => route.params.id,
  (newId) => {
    if (newId && typeof newId === "string") loadVacancy(newId);
  },
);
</script>
