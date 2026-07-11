import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import StatusBadge from "@/components/common/StatusBadge.vue";

describe("StatusBadge", () => {
  it("renders slot content", () => {
    const wrapper = mount(StatusBadge, { slots: { default: "Active" } });
    expect(wrapper.text()).toBe("Active");
  });

  it("applies green variant class", () => {
    const wrapper = mount(StatusBadge, {
      props: { variant: "green" },
      slots: { default: "OK" },
    });
    expect(wrapper.classes()).toContain("bg-green-100");
    expect(wrapper.classes()).toContain("text-green-800");
  });

  it("applies yellow variant class", () => {
    const wrapper = mount(StatusBadge, {
      props: { variant: "yellow" },
      slots: { default: "Pending" },
    });
    expect(wrapper.classes()).toContain("bg-yellow-100");
    expect(wrapper.classes()).toContain("text-yellow-800");
  });

  it("applies red variant class", () => {
    const wrapper = mount(StatusBadge, {
      props: { variant: "red" },
      slots: { default: "Error" },
    });
    expect(wrapper.classes()).toContain("bg-red-100");
    expect(wrapper.classes()).toContain("text-red-800");
  });

  it("applies blue variant class", () => {
    const wrapper = mount(StatusBadge, {
      props: { variant: "blue" },
      slots: { default: "Info" },
    });
    expect(wrapper.classes()).toContain("bg-blue-100");
    expect(wrapper.classes()).toContain("text-blue-800");
  });

  it("defaults to gray when no variant provided", () => {
    const wrapper = mount(StatusBadge, {
      slots: { default: "Default" },
    });
    expect(wrapper.classes()).toContain("bg-gray-100");
    expect(wrapper.classes()).toContain("text-gray-800");
  });
});
