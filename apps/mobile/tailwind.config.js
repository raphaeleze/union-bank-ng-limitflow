/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./app/**/*.{js,jsx,ts,tsx}", "./src/**/*.{js,jsx,ts,tsx}"],
  presets: [require("nativewind/preset")],
  darkMode: "media",
  theme: {
    extend: {
      colors: {
        ink: { DEFAULT: "#151222", dark: "#f2f0fa" },
        "ink-muted": { DEFAULT: "#6b6580", dark: "#a199c2" },
        surface: { DEFAULT: "#faf9fe", dark: "#0f0b22" },
        card: { DEFAULT: "#ffffff", dark: "#1b1533" },
        border: { DEFAULT: "#ece8f6", dark: "#2a2350" },
        accent: { DEFAULT: "#5b3df5", dark: "#8b7cff" },
        "accent-deep": { DEFAULT: "#241653", dark: "#150c33" },
        "accent-soft": { DEFAULT: "#efebff", dark: "#241c4a" },
        success: { DEFAULT: "#12b76a", dark: "#3dd68c" },
        "success-soft": { DEFAULT: "#e7f9f0", dark: "#123326" },
        "success-strong": { DEFAULT: "#0a7a4a", dark: "#3dd68c" },
        danger: { DEFAULT: "#e5484d", dark: "#ff6369" },
        "danger-soft": { DEFAULT: "#fdedee", dark: "#3a1518" },
        "danger-strong": { DEFAULT: "#b3231e", dark: "#ff6369" },
        warning: { DEFAULT: "#f5a524", dark: "#ffc24d" },
        "warning-soft": { DEFAULT: "#fef3e2", dark: "#3a2a10" },
        "warning-strong": { DEFAULT: "#8a5a10", dark: "#ffc24d" },
      },
    },
  },
  plugins: [],
};
