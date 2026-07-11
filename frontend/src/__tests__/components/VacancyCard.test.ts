import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import VacancyCard from "@/components/vacancies/VacancyCard.vue";

const defaultProps = {
  title: "Frontend Developer",
  companyName: "Acme Corp",
  salaryMin: null,
  salaryMax: null,
  postedAt: "2024-06-01",
};

function mountCard(propsOverrides: Record<string, any> = {}) {
  return mount(VacancyCard, {
    props: { ...defaultProps, ...propsOverrides },
    global: {
      stubs: { StatusBadge: { template: "<span><slot /></span>" } },
    },
  });
}

describe("VacancyCard", () => {
  it("renders title and company name", () => {
    const wrapper = mountCard();
    expect(wrapper.text()).toContain("Frontend Developer");
    expect(wrapper.text()).toContain("Acme Corp");
  });

  it("displays salary when both min and max are provided", () => {
    const wrapper = mountCard({
      salaryMin: 1000,
      salaryMax: 5000,
      salaryCurrency: "USD",
    });
    expect(wrapper.text()).toContain("1,000");
    expect(wrapper.text()).toContain("5,000");
    expect(wrapper.text()).toContain("USD");
  });

  it("displays 'from X' when only min is provided", () => {
    const wrapper = mountCard({
      salaryMin: 2000,
      salaryMax: null,
      salaryCurrency: "EUR",
    });
    expect(wrapper.text()).toContain("from");
    expect(wrapper.text()).toContain("2,000");
    expect(wrapper.text()).toContain("EUR");
  });

  it("displays 'up to X' when only max is provided", () => {
    const wrapper = mountCard({
      salaryMin: null,
      salaryMax: 8000,
      salaryCurrency: "RUB",
    });
    expect(wrapper.text()).toContain("up to");
    expect(wrapper.text()).toContain("8,000");
    expect(wrapper.text()).toContain("RUB");
  });

  it("displays no salary when both min and max are null", () => {
    const wrapper = mountCard({
      salaryMin: null,
      salaryMax: null,
    });
    const salaryEl = wrapper.find(".text-green-700");
    expect(salaryEl.exists()).toBe(false);
  });

  it("renders skills badges", () => {
    const wrapper = mountCard({
      skills: ["Vue", "TypeScript", "Tailwind"],
    });
    const text = wrapper.text();
    expect(text).toContain("Vue");
    expect(text).toContain("TypeScript");
    expect(text).toContain("Tailwind");
  });

  it("renders employmentType badge", () => {
    const wrapper = mountCard({ employmentType: "full-time" });
    expect(wrapper.text()).toContain("full-time");
  });

  it("does not render employmentType badge when null", () => {
    const wrapper = mountCard({ employmentType: null });
    expect(wrapper.text()).not.toContain("full-time");
  });

  it("renders Remote badge when remote is true", () => {
    const wrapper = mountCard({ remote: true });
    expect(wrapper.text()).toContain("Remote");
  });

  it("does not render Remote badge when remote is false", () => {
    const wrapper = mountCard({ remote: false });
    expect(wrapper.text()).not.toContain("Remote");
  });

  it("renders location", () => {
    const wrapper = mountCard({ location: "Moscow, Russia" });
    expect(wrapper.text()).toContain("Moscow, Russia");
  });

  it("renders postedAt", () => {
    const wrapper = mountCard({ postedAt: "2024-06-01" });
    expect(wrapper.text()).toContain("2024-06-01");
  });
});
