import { describe, it, expect } from "vitest";
import { mount } from "@vue/test-utils";
import Spinner from "@/components/common/Spinner.vue";

describe("Spinner", () => {
  it("renders the spinner element", () => {
    const wrapper = mount(Spinner);
    expect(wrapper.find("div").exists()).toBe(true);
  });

  it("contains an animate-spin element", () => {
    const wrapper = mount(Spinner);
    const spinner = wrapper.find(".animate-spin");
    expect(spinner.exists()).toBe(true);
  });
});
