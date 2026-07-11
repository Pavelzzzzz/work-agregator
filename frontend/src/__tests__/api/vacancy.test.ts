import { describe, it, expect, vi, beforeEach } from "vitest";

vi.mock("@/api/client", () => ({
  default: {
    get: vi.fn(),
  },
}));

import apiClient from "@/api/client";
import { vacancyApi } from "@/api/vacancy";

const mockedGet = vi.mocked(apiClient.get);

describe("vacancyApi", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe("search", () => {
    it("sends request with all filter fields", async () => {
      mockedGet.mockResolvedValueOnce({
        data: { total: 1, results: [], page: 1, pageSize: 20 },
      });

      await vacancyApi.search({
        query: "vue",
        language: "en",
        source: "hh",
        employmentType: "full-time",
        minSalary: 1000,
        maxSalary: 5000,
        skills: ["vue", "typescript"],
        companyName: "Acme",
        location: "Moscow",
        remoteOnly: true,
        page: 2,
        pageSize: 10,
      });

      expect(mockedGet).toHaveBeenCalledWith("/vacancies/search", {
        params: {
          q: "vue",
          language: "en",
          source: "hh",
          employmentType: "full-time",
          minSalary: 1000,
          maxSalary: 5000,
          skills: "vue,typescript",
          companyName: "Acme",
          location: "Moscow",
          remoteOnly: true,
          page: 2,
          pageSize: 10,
        },
      });
    });

    it("excludes undefined/null optional fields", async () => {
      mockedGet.mockResolvedValueOnce({
        data: { total: 0, results: [], page: 1, pageSize: 20 },
      });

      await vacancyApi.search({ query: "react" });

      expect(mockedGet).toHaveBeenCalledWith("/vacancies/search", {
        params: { q: "react" },
      });
    });

    it("does not include remoteOnly when false", async () => {
      mockedGet.mockResolvedValueOnce({
        data: { total: 0, results: [], page: 1, pageSize: 20 },
      });

      await vacancyApi.search({ query: "react", remoteOnly: false });

      expect(mockedGet).toHaveBeenCalledWith("/vacancies/search", {
        params: { q: "react" },
      });
    });

    it("returns the response data", async () => {
      const response = { total: 5, results: [{ id: "1" }], page: 1, pageSize: 20 };
      mockedGet.mockResolvedValueOnce({ data: response });

      const result = await vacancyApi.search({ query: "angular" });
      expect(result).toEqual(response);
    });

    it("sends empty skills array as no skills param", async () => {
      mockedGet.mockResolvedValueOnce({
        data: { total: 0, results: [], page: 1, pageSize: 20 },
      });

      await vacancyApi.search({ skills: [] });

      expect(mockedGet).toHaveBeenCalledWith("/vacancies/search", {
        params: {},
      });
    });
  });

  describe("subscribeUpdates", () => {
    it("creates EventSource and calls onEvent with parsed data", () => {
      const mockClose = vi.fn();
      let capturedInstance: any = null;

      vi.stubGlobal(
        "EventSource",
        class MockEventSource {
          onmessage: ((msg: MessageEvent) => void) | null = null;
          close = mockClose;
          constructor(_url: string) {
            capturedInstance = this;
          }
        },
      );

      const onEvent = vi.fn();
      const unsubscribe = vacancyApi.subscribeUpdates(onEvent);

      expect(capturedInstance).not.toBeNull();

      const fakeEvent = {
        data: '{"eventType":"NEW","vacancy":{},"timestamp":"2024-01-01"}',
      };
      capturedInstance.onmessage(fakeEvent as MessageEvent);

      expect(onEvent).toHaveBeenCalledWith({
        eventType: "NEW",
        vacancy: {},
        timestamp: "2024-01-01",
      });

      unsubscribe();
      expect(mockClose).toHaveBeenCalled();

      vi.unstubAllGlobals();
    });

    it("ignores JSON parse errors", () => {
      const mockClose = vi.fn();
      let capturedInstance: any = null;

      vi.stubGlobal(
        "EventSource",
        class MockEventSource {
          onmessage: ((msg: MessageEvent) => void) | null = null;
          close = mockClose;
          constructor(_url: string) {
            capturedInstance = this;
          }
        },
      );

      const onEvent = vi.fn();
      const unsubscribe = vacancyApi.subscribeUpdates(onEvent);

      capturedInstance.onmessage({ data: "invalid json" } as MessageEvent);

      expect(onEvent).not.toHaveBeenCalled();
      unsubscribe();
      vi.unstubAllGlobals();
    });
  });
});
