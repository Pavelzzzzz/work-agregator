<template>
  <router-link
    :to="`/vacancies/${id}`"
    class="block border rounded-lg p-4 hover:shadow-md transition-shadow bg-white cursor-pointer"
  >
    <div class="flex justify-between items-start gap-4">
      <div class="flex-1 min-w-0">
        <h3 class="font-semibold text-gray-900 truncate">{{ title }}</h3>
        <p class="text-sm text-gray-500 mt-0.5">{{ companyName }}</p>
      </div>
      <div v-if="salary" class="text-sm font-medium text-green-700 whitespace-nowrap">
        {{ salary }}
      </div>
    </div>
    <div class="flex flex-wrap gap-1.5 mt-3">
      <StatusBadge v-if="employmentType" variant="blue">{{ employmentType }}</StatusBadge>
      <StatusBadge v-if="remote" variant="green">Remote</StatusBadge>
      <StatusBadge v-for="skill in skills" :key="skill" variant="gray">{{ skill }}</StatusBadge>
    </div>
    <div class="flex justify-between items-center mt-3 text-xs text-gray-400">
      <span>{{ location }}</span>
      <span>{{ postedAt }}</span>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from "vue";
import StatusBadge from "@/components/common/StatusBadge.vue";

const props = defineProps<{
  id: string;
  title: string;
  companyName: string;
  employmentType?: string | null;
  remote?: boolean;
  skills?: string[];
  location?: string | null;
  salaryMin?: number | null;
  salaryMax?: number | null;
  salaryCurrency?: string | null;
  postedAt: string;
}>();

const salary = computed(() => {
  const { salaryMin, salaryMax, salaryCurrency } = props;
  if (salaryMin === null && salaryMax === null) return null;
  const fmt = (v: number) => v.toLocaleString();
  const cur = salaryCurrency || "";
  if (salaryMin !== null && salaryMax !== null) return `${fmt(salaryMin)}–${fmt(salaryMax)} ${cur}`;
  if (salaryMin !== null) return `from ${fmt(salaryMin)} ${cur}`;
  return `up to ${fmt(salaryMax!)} ${cur}`;
});
</script>
