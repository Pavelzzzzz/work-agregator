import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '@/components/dashboard/Dashboard.vue';
import VacancyList from '@/components/vacancies/VacancyList.vue';
import CompanyList from '@/components/companies/CompanyList.vue';

const routes = [
  { path: '/', name: 'dashboard', component: Dashboard },
  { path: '/vacancies', name: 'vacancies', component: VacancyList },
  { path: '/companies', name: 'companies', component: CompanyList },
];

const router = createRouter({ history: createWebHistory(), routes });
export default router;
