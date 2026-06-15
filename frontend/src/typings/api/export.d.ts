declare namespace Api {
  namespace Export {
    // NOTE: Async export task types removed — backend only provides synchronous CSV/Excel download
    // via GET /export/csv and GET /export/excel (DataExportController).
    // Re-add these types if async task-based export is implemented in the future.
  }
}
