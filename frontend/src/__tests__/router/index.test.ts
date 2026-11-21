import { describe, expect, it } from "vitest";
import router from "@/router";

describe("router", () => {
  it("passes company name from URL to CompanyDetail as a param", async () => {
    await router.push("/companies/EPAM%20Systems");
    await router.isReady();

    expect(router.currentRoute.value.name).toBe("company-detail");
    expect(router.currentRoute.value.params.name).toBe("EPAM Systems");
  });
});
