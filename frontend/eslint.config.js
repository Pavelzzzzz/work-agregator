import js from "@eslint/js";
import tseslint from "typescript-eslint";
import pluginVue from "eslint-plugin-vue";
import vueParser from "vue-eslint-parser";

export default [
  js.configs.recommended,
  ...tseslint.configs.recommended,
  ...pluginVue.configs["flat/recommended"],
  {
    files: ["**/*.vue"],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tseslint.parser,
        ecmaVersion: "latest",
        sourceType: "module",
      },
    },
  },
  {
    rules: {
      "vue/no-v-html": "off",
      "vue/html-self-closing": "off",
      "vue/multi-word-component-names": "off",
      "vue/singleline-html-element-content-newline": "off",
      "vue/max-attributes-per-line": "off",
      "vue/first-attribute-linebreak": "off",
      "vue/html-indent": "off",
      "vue/html-closing-bracket-newline": "off",
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_" },
      ],
      "@typescript-eslint/no-explicit-any": "warn",
      "no-console": "warn",
    },
  },
  {
    files: ["**/__tests__/**"],
    rules: {
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/no-this-alias": "off",
    },
  },
  {
    ignores: ["dist/", "node_modules/"],
  },
];
