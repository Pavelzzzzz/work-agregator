<template>
  <div>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Dashboard</h1>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
      <div class="bg-white rounded-lg border p-5">
        <p class="text-sm text-gray-500">Companies</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ companyCount }}</p>
      </div>
      <div class="bg-white rounded-lg border p-5">
        <p class="text-sm text-gray-500">Vacancies</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ vacancyCount }}</p>
      </div>
      <div class="bg-white rounded-lg border p-5">
        <p class="text-sm text-gray-500">Updates (SSE)</p>
        <p class="text-3xl font-bold text-gray-900 mt-1">{{ eventCount }}</p>
      </div>
    </div>
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white rounded-lg border">
        <div class="px-5 py-3 border-b flex justify-between items-center">
          <h2 class="font-semibold text-gray-900">Companies</h2>
          <router-link to="/companies" class="text-sm text-blue-600 hover:underline">
            View all
          </router-link>
        </div>
        <div v-if="companyStore.loading" class="p-5"><Spinner /></div>
        <div v-else-if="companyStore.error" class="p-5 text-sm text-red-600">
          {{ companyStore.error }}
        </div>
        <div v-else>
          <div
            v-for="c in companyStore.companies"
            :key="c.id"
            class="flex justify-between items-center px-5 py-3 border-b last:border-b-0 hover:bg-gray-50"
          >
            <div>
              <p class="font-medium text-gray-900">{{ c.name }}</p>
              <p class="text-xs text-gray-500">{{ c.description }}</p>
            </div>
            <StatusBadge :variant="c.isActive ? 'green' : 'gray'">{{ c.scanStatus }}</StatusBadge>
          </div>
          <div v-if="!companyStore.companies.length" class="p-5 text-sm text-gray-400 text-center">
            No companies yet
          </div>
        </div>
      </div>
      <div class="bg-white rounded-lg border">
        <div class="px-5 py-3 border-b flex justify-between items-center">
          <h2 class="font-semibold text-gray-900">Live Updates</h2>
          <button class="text-sm text-gray-500 hover:text-gray-700" @click="clearEvents">
            Clear
          </button>
        </div>
        <div v-if="vacancyStore.events.length" class="divide-y max-h-80 overflow-y-auto">
          <div v-for="(evt, i) in vacancyStore.events" :key="i" class="px-5 py-2.5 text-sm">
            <StatusBadge :variant="evt.eventType === 'NEW' ? 'green' : 'yellow'" class="mr-2">
              {{ evt.eventType }}
            </StatusBadge>
            <span class="text-gray-700">{{ evt.vacancy?.title || "Unknown title" }}</span>
            <span class="text-gray-400 ml-2 text-xs">{{ formatTime(evt.timestamp) }}</span>
          </div>
        </div>
        <div v-else class="p-5 text-sm text-gray-400 text-center">Waiting for updates...</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted } from "vue";
import { useCompanyStore } from "@/stores/company";
import { useVacancyStore } from "@/stores/vacancy";
import Spinner from "@/components/common/Spinner.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";

const companyStore = useCompanyStore();
const vacancyStore = useVacancyStore();

const companyCount = computed(() => companyStore.companies.length);
const vacancyCount = computed(() => vacancyStore.total);
const eventCount = computed(() => vacancyStore.events.length);

function formatTime(ts: string) {
  try {
    return new Date(ts).toLocaleTimeString();
  } catch {
    return "";
  }
}

function clearEvents() {
  vacancyStore.events.value = [];
}

onMounted(() => {
  companyStore.fetchAll();
  vacancyStore.search({ pageSize: 0 });
  vacancyStore.subscribe();
});

onUnmounted(() => {
  vacancyStore.unsubscribeUpdates();
});
</script>
