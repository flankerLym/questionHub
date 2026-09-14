import { test, expect } from '@playwright/test'

test('login page renders and accepts the default password', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: '问题归档系统' })).toBeVisible()
  await page.getByLabel('访问口令').fill('140810921')
  await page.getByRole('button', { name: '进入归档' }).click()
  await expect(page.getByText('Local knowledge archive')).toBeVisible()
})
