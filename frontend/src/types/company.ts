export interface Company {
  id: string;
  name: string;
  careersUrl: string;
  websiteUrl: string;
  description: string;
  active: boolean;
  lastScanAt: string | null;
  scanStatus: string;
  scanError: string | null;
  createdAt: string;
  updatedAt: string;
}
