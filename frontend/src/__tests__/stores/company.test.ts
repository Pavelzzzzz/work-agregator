import { describe, it, expect, vi, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";

vi.mock("@/api/company", () => ({
  companyApi: {
    list: vi.fn(),
  },
}));

import { useCompanyStore } from "@/stores/company";
import { companyApi } from "@/api/company";

const mockedList = vi.mocked(companyApi.list);

describe("company store", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setActivePinia(createPinia());
  });

  describe("fetchAll", () => {
    it("sets companies on success", async () => {
      const companies = [
        { id: "1", name: "Acme" },
        { id: "2", name: "Globex" },
      ];
      mockedList.mockResolvedValueOnce(companies as any);

      const store = useCompanyStore();
      expect(store.loading).toBe(false);

      const promise = store.fetchAll();
      expect(store.loading).toBe(true);

      await promise;

      expect(store.companies).toEqual(companies);
      expect(store.loading).toBe(false);
      expect(store.error).toBeNull();
    });

    it("sets error message on failure", async () => {
      mockedList.mockRejectedValueOnce(new Error("Server down"));

      const store = useCompanyStore();
      await store.fetchAll();

      expect(store.error).toBe("Server down");
      expect(store.loading).toBe(false);
      expect(store.companies).toEqual([]);
    });

    it("sets generic error for non-Error exceptions", async () => {
      mockedList.mockRejectedValueOnce("string error");

      const store = useCompanyStore();
      await store.fetchAll();

      expect(store.error).toBe("Failed to load companies");
    });

    it("clears error before new fetch", async () => {
      mockedList.mockRejectedValueOnce(new Error("fail"));
      const store = useCompanyStore();
      await store.fetchAll();
      expect(store.error).toBe("fail");

      mockedList.mockResolvedValueOnce([{ id: "1", name: "A" }] as any);
      await store.fetchAll();
      expect(store.error).toBeNull();
    });
  });
});
