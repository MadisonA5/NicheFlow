export default {
  mutate: ['src/features/search/filterListings.js'],
  testRunner: 'vitest',
  vitest: {
    configFile: 'vite.config.js',
  },
  reporters: ['clear-text', 'progress', 'html'],
}
