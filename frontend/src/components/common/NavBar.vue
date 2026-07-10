<template>
  <nav class="bg-gray-900 text-white">
    <div class="container flex items-center justify-between h-14">
      <router-link to="/" class="text-lg font-bold tracking-tight">Vacancy Scout</router-link>
      <div class="flex gap-6 text-sm">
        <router-link to="/" class="hover:text-blue-400 transition-colors">Dashboard</router-link>
        <router-link to="/vacancies" class="hover:text-blue-400 transition-colors"
          >
Vacancies
</router-link
        >
        <router-link to="/companies" class="hover:text-blue-400 transition-colors"
          >
Companies
</router-link
        >
      </div>
    </div>
  </nav>
</template>
