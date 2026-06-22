/**
 * TapTrack App Control - Google Apps Script
 *
 * Setup:
 *   1. Open your Google Sheet → Extensions → Apps Script
 *   2. Paste this entire file into the editor (replace existing code)
 *   3. Deploy → New deployment → Web app
 *      - Execute as: Me
 *      - Who has access: Anyone
 *   4. Copy the web app URL and update APP_CONTROL_CSV_URL in UpdateChecker.kt
 *
 * Re-deploying after changes:
 *   Deploy → Manage deployments → Edit (pencil icon) → New version → Deploy
 */

function doGet() {
  const sheet = SpreadsheetApp.getActiveSpreadsheet()
    .getSheetByName('AppControl');

  if (!sheet) {
    return ContentService
      .createTextOutput(JSON.stringify({ error: 'AppControl sheet not found' }))
      .setMimeType(ContentService.MimeType.JSON);
  }

  const rows = sheet.getDataRange().getValues();
  const result = {};

  for (let i = 1; i < rows.length; i++) {
    const key = String(rows[i][0]).trim();
    const value = String(rows[i][1]).trim();
    if (key) result[key] = value;
  }

  return ContentService
    .createTextOutput(JSON.stringify(result))
    .setMimeType(ContentService.MimeType.JSON);
}
