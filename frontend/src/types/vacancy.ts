export interface VacancyTranslation {
  id: string;
  vacancyId: string;
  lang: string;
  title: string;
  companyName: string;
  description: string | null;
  requirements: string | null;
  responsibilities: string | null;
}

export interface Vacancy {
  id: string;
  sourceUrl: string;
  sourceName: string;
  employmentType: string | null;
  skills: string[];
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  location: string | null;
  remote: boolean;
  tags: string[];
  companyId: string | null;
  postedAt: string;
  createdAt: string;
  updatedAt: string;
  translations: VacancyTranslation[];
}

export interface SearchFilters {
  query?: string;
  source?: string;
  employmentType?: string;
  minSalary?: number;
  maxSalary?: number;
  skills?: string[];
  companyId?: string;
  location?: string;
  remoteOnly?: boolean;
  language?: string;
  page?: number;
  pageSize?: number;
}

export interface SearchResponse {
  total: number;
  results: Vacancy[];
  page: number;
  pageSize: number;
}

export interface VacancyUpdateEvent {
  eventType: "NEW" | "UPDATED";
  vacancy: Vacancy;
  timestamp: string;
}
