import { createRouter, createWebHistory } from "vue-router";
import Dashboard from "@/components/dashboard/Dashboard.vue";
import VacancyList from "@/components/vacancies/VacancyList.vue";
import VacancyDetail from "@/components/vacancies/VacancyDetail.vue";
import CompanyList from "@/components/companies/CompanyList.vue";
import CompanyDetail from "@/components/companies/CompanyDetail.vue";

const routes = [
  { path: "/", name: "dashboard", component: Dashboard },
  { path: "/vacancies", name: "vacancies", component: VacancyList },
  { path: "/vacancies/:id", name: "vacancy-detail", component: VacancyDetail },
  { path: "/companies", name: "companies", component: CompanyList },
  { path: "/companies/:id", name: "company-detail", component: CompanyDetail },
];

const router = createRouter({ history: createWebHistory(), routes });
export default router;
