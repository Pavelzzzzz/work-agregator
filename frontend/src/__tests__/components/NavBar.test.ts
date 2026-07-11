import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import { createRouter, createMemoryHistory } from "vue-router";
import NavBar from "@/components/common/NavBar.vue";

const router = createRouter({
  history: createMemoryHistory(),
  routes: [
    { path: "/", component: { template: "<div />" } },
    { path: "/vacancies", component: { template: "<div />" } },
    { path: "/companies", component: { template: "<div />" } },
  ],
});

async function mountNavBar() {
  const wrapper = mount(NavBar, {
    global: {
      plugins: [router],
    },
  });
  await router.isReady();
  return wrapper;
}

describe("NavBar", () => {
  it("renders the brand name", async () => {
    const wrapper = await mountNavBar();
    expect(wrapper.text()).toContain("Vacancy Scout");
  });

  it("renders Dashboard link", async () => {
    const wrapper = await mountNavBar();
    const links = wrapper.findAll("a");
    expect(links.some((l) => l.text().includes("Dashboard"))).toBe(true);
  });

  it("renders Vacancies link", async () => {
    const wrapper = await mountNavBar();
    const links = wrapper.findAll("a");
    expect(links.some((l) => l.text().includes("Vacancies"))).toBe(true);
  });

  it("renders Companies link", async () => {
    const wrapper = await mountNavBar();
    const links = wrapper.findAll("a");
    expect(links.some((l) => l.text().includes("Companies"))).toBe(true);
  });

  it("renders 4 router-links total (brand + 3 nav links)", async () => {
    const wrapper = await mountNavBar();
    const links = wrapper.findAll("a");
    expect(links).toHaveLength(4);
  });
});
