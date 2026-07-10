<template>
  <div>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">Companies</h1>
    <div v-if="store.loading"><Spinner /></div>
    <div v-else-if="store.error" class="text-red-600 text-sm p-4 bg-red-50 rounded-lg">
      {{ store.error }}
    </div>
    <div v-else-if="!store.companies.length" class="text-gray-400 text-center py-12">
      No companies found.
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="c in store.companies"
        :key="c.id"
        class="border rounded-lg p-5 bg-white hover:shadow-md transition-shadow"
      >
        <div class="flex justify-between items-start">
          <div>
            <h3 class="font-semibold text-gray-900">{{ c.name }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ c.description }}</p>
          </div>
          <StatusBadge :variant="c.active ? 'green' : 'gray'">{{ c.scanStatus }}</StatusBadge>
        </div>
        <div class="mt-4 text-sm">
          <a
            v-if="c.careersUrl"
            :href="c.careersUrl"
            target="_blank"
            class="text-blue-600 hover:underline block"
            >Careers page →</a
          >
          <a
            v-if="c.websiteUrl"
            :href="c.websiteUrl"
            target="_blank"
            class="text-gray-500 hover:underline block mt-1"
            >Website →</a
          >
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { useCompanyStore } from "@/stores/company";
import Spinner from "@/components/common/Spinner.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";

const store = useCompanyStore();
onMounted(() => store.fetchAll());
</script>
