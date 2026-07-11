import { describe, it, expect, vi, beforeEach } from "vitest";
import { setActivePinia, createPinia } from "pinia";

vi.mock("@/api/vacancy", () => ({
  vacancyApi: {
    search: vi.fn(),
    subscribeUpdates: vi.fn(),
  },
}));

import { useVacancyStore } from "@/stores/vacancy";
import { vacancyApi } from "@/api/vacancy";

const mockedSearch = vi.mocked(vacancyApi.search);
const mockedSubscribeUpdates = vi.mocked(vacancyApi.subscribeUpdates);

describe("vacancy store", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    setActivePinia(createPinia());
  });

  describe("search", () => {
    it("sets results, total, page, pageSize on success", async () => {
      mockedSearch.mockResolvedValueOnce({
        total: 42,
        results: [{ id: "1" }] as any,
        page: 2,
        pageSize: 10,
      });

      const store = useVacancyStore();
      expect(store.loading).toBe(false);

      const promise = store.search({ query: "vue", page: 2, pageSize: 10 });
      expect(store.loading).toBe(true);

      await promise;

      expect(store.results).toEqual([{ id: "1" }]);
      expect(store.total).toBe(42);
      expect(store.page).toBe(2);
      expect(store.pageSize).toBe(10);
      expect(store.loading).toBe(false);
      expect(store.error).toBeNull();
    });

    it("sets error message on failure", async () => {
      mockedSearch.mockRejectedValueOnce(new Error("Network error"));

      const store = useVacancyStore();
      await store.search({ query: "react" });

      expect(store.error).toBe("Network error");
      expect(store.loading).toBe(false);
      expect(store.results).toEqual([]);
    });

    it("sets generic error for non-Error exceptions", async () => {
      mockedSearch.mockRejectedValueOnce("string error");

      const store = useVacancyStore();
      await store.search({});

      expect(store.error).toBe("Search failed");
    });

    it("clears error before new search", async () => {
      mockedSearch.mockRejectedValueOnce(new Error("fail"));
      const store = useVacancyStore();
      await store.search({});
      expect(store.error).toBe("fail");

      mockedSearch.mockResolvedValueOnce({
        total: 0,
        results: [],
        page: 1,
        pageSize: 20,
      });
      await store.search({});
      expect(store.error).toBeNull();
    });
  });

  describe("subscribe", () => {
    it("calls vacancyApi.subscribeUpdates", () => {
      mockedSubscribeUpdates.mockReturnValueOnce(vi.fn());

      const store = useVacancyStore();
      store.subscribe();

      expect(mockedSubscribeUpdates).toHaveBeenCalled();
    });

    it("does not subscribe twice", () => {
      const unsub = vi.fn();
      mockedSubscribeUpdates.mockReturnValue(unsub);

      const store = useVacancyStore();
      store.subscribe();
      store.subscribe();

      expect(mockedSubscribeUpdates).toHaveBeenCalledTimes(1);
    });

    it("adds events to events array", () => {
      let callback: any;
      mockedSubscribeUpdates.mockImplementation((cb: any) => {
        callback = cb;
        return vi.fn();
      });

      const store = useVacancyStore();
      store.subscribe();

      const evt = { eventType: "NEW", vacancy: { id: "1" }, timestamp: "2024-01-01" };
      callback(evt);

      expect(store.events).toHaveLength(1);
      expect(store.events[0]).toEqual(evt);
    });

    it("limits events to 100", () => {
      let callback: any;
      mockedSubscribeUpdates.mockImplementation((cb: any) => {
        callback = cb;
        return vi.fn();
      });

      const store = useVacancyStore();
      store.subscribe();

      for (let i = 0; i < 110; i++) {
        callback({ eventType: "NEW", vacancy: { id: String(i) }, timestamp: "" });
      }

      expect(store.events).toHaveLength(100);
      expect(store.events[0].vacancy.id).toBe("109");
    });
  });

  describe("unsubscribeUpdates", () => {
    it("calls unsubscribe function", () => {
      const unsub = vi.fn();
      mockedSubscribeUpdates.mockReturnValue(unsub);

      const store = useVacancyStore();
      store.subscribe();
      store.unsubscribeUpdates();

      expect(unsub).toHaveBeenCalled();
    });

    it("is safe to call without subscribe", () => {
      const store = useVacancyStore();
      expect(() => store.unsubscribeUpdates()).not.toThrow();
    });
  });
});
