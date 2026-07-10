export interface Vacancy {
  id: string;
  sourceId: string;
  sourceName: string;
  title: string;
  companyName: string;
  companyWebsite: string | null;
  description: string | null;
  requirements: string | null;
  responsibilities: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  location: string | null;
  employmentType: string | null;
  experienceRequired: string | null;
  skills: string[];
  postedAt: string;
  url: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SearchFilters {
  query?: string;
  language?: string;
  source?: string;
  employmentType?: string;
  minSalary?: number;
  maxSalary?: number;
  skills?: string[];
  companyName?: string;
  location?: string;
  remoteOnly?: boolean;
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
