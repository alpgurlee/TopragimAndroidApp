module.exports = {
  root: true,
  env: {
    es6: true,
    node: true,
  },
  parserOptions: {
    ecmaVersion: 2020, // veya 2021 veya daha yüksek bir sürüm
  },
  extends: [
    "eslint:recommended",
    "google",
  ],
  rules: {
    "indent": ["error", 2],
    "max-len": ["error", { "code": 120 }],
    "quotes": ["error", "double"],
    "object-curly-spacing": ["error", "always"],
  },
};
