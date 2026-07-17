const REPORT_PRINTING_CLASS = 'tch-report-printing';

export function exportReportPdf(): void {
  document.body.classList.add(REPORT_PRINTING_CLASS);
  window.print();
  window.setTimeout(() => document.body.classList.remove(REPORT_PRINTING_CLASS), 0);
}

export function exportReportCsv(filename: string, rows: readonly (readonly (string | number | null | undefined)[])[]): void {
  const csv = rows.map(row => row.map(csvCell).join(',')).join('\n');
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.style.display = 'none';
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}

function csvCell(value: string | number | null | undefined): string {
  const text = value == null ? '' : String(value);
  return /[",\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}
