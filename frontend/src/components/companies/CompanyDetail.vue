<template>
  <div>
    <router-link to="/companies" class="text-sm text-blue-600 hover:underline mb-4 inline-block">
      &larr; Back to companies
    </router-link>
    <div v-if="store.loading"><Spinner /></div>
    <div v-else-if="store.error" class="text-red-600 text-sm p-4 bg-red-50 rounded-lg">
      {{ store.error }}
    </div>
    <div v-else-if="c" class="bg-white border rounded-lg p-6">
      <div class="flex justify-between items-start">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ c.name }}</h1>
          <p v-if="c.description" class="text-gray-500 mt-1">{{ c.description }}</p>
        </div>
        <StatusBadge :variant="c.isActive ? 'green' : 'gray'">{{ c.scanStatus }}</StatusBadge>
      </div>

      <div class="mt-6 space-y-2 text-sm">
        <p v-if="c.lastScanAt"><strong>Last scan:</strong> {{ formatDate(c.lastScanAt) }}</p>
        <p v-if="c.scanError" class="text-red-600">
          <strong>Scan error:</strong> {{ c.scanError }}
        </p>
      </div>

      <div class="mt-6 flex gap-3">
        <a
          v-if="c.careersUrl"
          :href="c.careersUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="bg-blue-600 text-white px-5 py-2 rounded-lg text-sm font-medium hover:bg-blue-700"
        >
          Careers page &rarr;
        </a>
        <a
          v-if="c.websiteUrl"
          :href="c.websiteUrl"
          target="_blank"
          rel="noopener noreferrer"
          class="border border-gray-300 text-gray-700 px-5 py-2 rounded-lg text-sm font-medium hover:bg-gray-50"
        >
          Website &rarr;
        </a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { useCompanyStore } from "@/stores/company";
import Spinner from "@/components/common/Spinner.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";

const route = useRoute();
const store = useCompanyStore();
const name = route.params.name as string;

const c = computed(() => store.currentCompany);

function formatDate(d: string) {
  try {
    return new Date(d).toLocaleDateString();
  } catch {
    return d;
  }
}

onMounted(() => store.fetchByName(name));
</script>
