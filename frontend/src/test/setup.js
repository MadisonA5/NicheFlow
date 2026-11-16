import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'

afterEach(() => {
  cleanup()
  window.history.replaceState({}, '', '/')
})

// jsdom does not implement scrolling, so give elements a no-op version
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}
